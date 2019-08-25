package com.ustadmobile.core.controller

import androidx.paging.DataSource
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.CustomFieldDao
import com.ustadmobile.core.db.dao.CustomFieldValueDao
import com.ustadmobile.core.db.dao.CustomFieldValueOptionDao
import com.ustadmobile.core.db.dao.FeedEntryDao
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.core.view.ClazzDetailEnrollStudentView.Companion.ARG_NEW_PERSON
import com.ustadmobile.core.view.PersonDetailEnrollClazzView
import com.ustadmobile.core.view.PersonDetailView
import com.ustadmobile.core.view.PersonDetailView.Companion.ARG_PERSON_UID
import com.ustadmobile.core.view.PersonDetailViewField
import com.ustadmobile.core.view.PersonEditView
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_ADDRESS
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_ATTENDANCE
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_BIRTHDAY
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_CLASSES
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_FATHER_NAME
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_FATHER_NAME_AND_PHONE_NUMBER
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_FATHER_NUMBER
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_FIRST_NAMES
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_FULL_NAME
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_LAST_NAME
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_MOTHER_NAME
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_MOTHER_NAME_AND_PHONE_NUMBER
import com.ustadmobile.lib.db.entities.PersonDetailPresenterField.Companion.PERSON_FIELD_UID_MOTHER_NUMBER
import com.ustadmobile.lib.db.entities.PersonField.Companion.FIELD_TYPE_HEADER
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch

/**
 * PersonEditPresenter : This is responsible for generating the Edit data along with its Custom
 * Fields. It is also responsible for updating the data and checking for changes and handling
 * Done with Save or Discard.
 *
 */
class PersonEditPresenter
/**
 * Presenter's constructor where we are getting arguments and setting the newly/editable
 * personUid
 *
 * @param context Android context
 * @param arguments Arguments from the Activity passed here.
 * @param view  The view that called this presenter (PersonEditView->PersonEditActivity)
 */
(context: Any, arguments: Map<String, String>?, view: PersonEditView,
        val impl : UstadMobileSystemImpl = UstadMobileSystemImpl.instance)
    :UstadBaseController<PersonEditView>(context, arguments!!, view) {


    private var personLiveData: DoorLiveData<Person?>? = null

    //Headers and Fields
    private var headersAndFields: List<PersonDetailPresenterField>? = null

    var personUid: Long = 0

    private var mUpdatedPerson: Person? = null

    //OG person before Done/Save/Discard clicked.
    private var mOriginalValuePerson: Person? = null

    private var assignedClazzes: DataSource.Factory<Int, ClazzWithNumStudents>? = null

    //The custom fields' values
    private val customFieldWithFieldValueMap: Map<Long, PersonCustomFieldWithPersonCustomFieldValue>? = null

    internal var repository = UmAccountManager.getRepositoryForActiveAccount(context)

    private val personDao = repository.personDao

    private var newPersonString = ""

    private val customFieldsToUpdate: MutableList<PersonCustomFieldValue>

    private val personCustomFieldValueDao = repository.personCustomFieldValueDao

    private var loggedInPersonUid: Long? = 0L

    private val viewIdToCustomFieldUid: HashMap<Int, Long>

    private var customFieldDao: CustomFieldDao? = null
    private var customFieldValueDao: CustomFieldValueDao? = null
    private var optionDao: CustomFieldValueOptionDao? = null

    private val customFieldDropDownOptions: HashMap<Long, List<String>>

    init {

        if (arguments!!.containsKey(ARG_PERSON_UID)) {
            personUid = (arguments!!.get(ARG_PERSON_UID)!!.toString()).toLong()
        }

        if (arguments!!.containsKey(ARG_NEW_PERSON)) {
            newPersonString = arguments!!.get(ARG_NEW_PERSON)!!.toString()
        }

        customFieldsToUpdate = ArrayList()

        viewIdToCustomFieldUid = HashMap()

        customFieldDropDownOptions = HashMap()

    }

    fun addToMap(viewId: Int, fieldId: Long) {
        viewIdToCustomFieldUid[viewId] = fieldId
    }

    /**
     * Getting custom fields (new way)
     */
    private fun getAllPersonCustomFields() {
        //0. Clear all added custom fields on view.
        view.runOnUiThread(Runnable{ view.clearAllCustomFields() })

        GlobalScope.launch {
            //1. Get all custom fields
            val result = customFieldDao!!.findAllCustomFieldsProviderForEntityAsync(Person.TABLE_ID)
            for (c in result!!) {
                //Get value as well
                val result2 = customFieldValueDao!!.findValueByCustomFieldUidAndEntityUid(c.customFieldUid, personUid)
                var valueString: String? = ""
                var valueSelection = 0

                if (c.customFieldType == CustomField.FIELD_TYPE_TEXT) {

                    if (result2 != null) {
                        valueString = result2.customFieldValueValue
                    }
                    val finalValueString = arrayOf<String>(valueString!!)
                    view.runOnUiThread(Runnable{

                        view.addCustomFieldText(c, finalValueString[0])
                        //view.addComponent(finalValueString[0], c.getCustomFieldName());
                    })

                } else if (c.customFieldType == CustomField.FIELD_TYPE_DROPDOWN) {
                    if (result2 != null) {
                        try {
                            valueSelection = (result2.customFieldValueValue!!).toInt()
                        } catch (nfe: NumberFormatException) {
                            valueSelection = 0
                        }

                    }
                    val finalValueSelection = valueSelection
                    val result3 = optionDao!!.findAllOptionsForFieldAsync(c.customFieldUid)
                    val options = ArrayList<String>()

                    for (o in result3!!) {
                        options.add(o.customFieldValueOptionName!!)
                    }
                    //Get value
                    var valueString = "-"
                    if (finalValueSelection > 0) {
                        valueString = options[finalValueSelection]
                    }
                    val finalValueString = valueString

                    customFieldDropDownOptions[c.customFieldUid] = options
                    view.runOnUiThread(Runnable{
                        //view.addComponent(finalValueString, c.getCustomFieldName());
                        val a = arrayOfNulls<String>(options.size)
                        options.toTypedArray()
                        view.addCustomFieldDropdown(c, a, finalValueSelection)
                        //view.addCustomFieldText(c, finalValueString);

                    })

                }

            }
        }
    }

    /**
     * Presenter's Overridden onCreate that: Gets the mPerson LiveData and observe it.
     * @param savedState    The saved state
     */
    fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)
        val personCustomFieldValueDao = repository.personCustomFieldValueDao
        val personDetailPresenterFieldDao = repository.personDetailPresenterFieldDao
        val personCustomFieldDao = repository.personCustomFieldDao

        customFieldDao = repository.customFieldDao
        customFieldValueDao = repository.customFieldValueDao
        optionDao = repository.customFieldValueOptionDao

        loggedInPersonUid = UmAccountManager.getActiveAccount(context)!!.personUid

        if (newPersonString == "true") {
            view.updateToolbarTitle(impl.getString(MessageID.new_person, context))
        }

        getAllPersonCustomFields()

        var thisP = this
        //Get all the currently set headers and fields:
        GlobalScope.launch {
            val result = personDetailPresenterFieldDao.findAllPersonDetailPresenterFieldsEditMode()

            //Remove old custom fields
            val fieldsIterator = result!!.iterator()
            while (fieldsIterator.hasNext()) {
                val field = fieldsIterator.next()
                val fieldIndex = field.fieldIndex
                if (fieldIndex == 19 || fieldIndex == 20 || fieldIndex == 21) {
                    //TODO: Remove from iterator in Kotlin
                    fieldsIterator.remove()
                }
            }

            headersAndFields = result

            //Get person live data and observe
            personLiveData = personDao.findByUidLive(personUid)
            //Observe the live data
            personLiveData!!.observe(thisP, thisP::handlePersonValueChanged)

        }

    }

    /**
     * Updates the pic of the person after taken to the Person object directly
     *
     * @param picPath    The whole path of the picture.
     */
    fun updatePersonPic(picPath: String) {
        //Find the person
        GlobalScope.launch {
            val personWithPic = personDao.findByUidAsync(personUid)

            val personPictureDao = repository.personPictureDao
            val personPicture = personPictureDao.findByPersonUidAsync(personWithPic!!.personUid)

            if (personPicture != null) {
                //TODO: KMP attachment :
                //personPictureDao.setAttachmentFromTmpFile(personPicture.personPictureUid, picPath)
            }

            //Update personWithpic
            personDao.updatePersonAsync(personWithPic, loggedInPersonUid!!)
            generateFeedsForPersonUpdate(repository, mUpdatedPerson!!)

        }
    }

    /**
     * Generates live data for Clazz list to be assigned to the current Person being edited.
     */
    fun generateAssignedClazzesLiveData() {
        val clazzDao = repository.clazzDao
        assignedClazzes = clazzDao.findAllClazzesByPersonUid(personUid)
        updateClazzListProviderToView()
    }

    /**
     * Updates the Clazz List provider of type ClazzWithNumStudents that is set on this Presenter to
     * the View.
     */
    private fun updateClazzListProviderToView() {
        view.setClazzListProvider(assignedClazzes!!)
    }

    private fun updatePersonPic(thisPerson: Person) {
        val personPictureDao = repository.personPictureDao
        GlobalScope.launch {
            val personPicture = personPictureDao.findByPersonUidAsync(thisPerson.personUid)
            if (personPicture != null) {
                //TODO: KMP Atachment
                view.updateImageOnView(personPictureDao.getAttachmentPath(
                        personPicture.personPictureUid))
            }
        }

    }

    /**
     * Common method to set edit fields up for the current Person Editing.
     *
     * @param thisPerson The person being edited
     * @param allFields The Fields
     * @param thisView  The View
     * @param valueMap  The Custom fields value map
     */
    private fun setFieldsOnView(thisPerson: Person, allFields: List<PersonDetailPresenterField>,
                                thisView: PersonEditView,
                                valueMap: Map<Long, PersonCustomFieldWithPersonCustomFieldValue>?) {

        //TODO: Locale on Kotlin Core
        val currnetLocale = Locale.getDefault()

        updatePersonPic(thisPerson)

        //Clear all view before setting fields ?
        view.clearAllFields()

        for (field in allFields) {

            var thisValue: String? = ""

            if (field.fieldType == FIELD_TYPE_HEADER) {
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(FIELD_TYPE_HEADER,
                                field.headerMessageId, null!!), field.headerMessageId)
                continue
            }

            if (field.fieldUid == PERSON_FIELD_UID_FULL_NAME.toLong()) {
                thisValue = thisPerson.firstNames + " " + thisPerson.lastName
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_FIRST_NAMES.toLong()) {
                thisValue = thisPerson.firstNames
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)

            } else if (field.fieldUid == PERSON_FIELD_UID_LAST_NAME.toLong()) {
                thisValue = thisPerson.lastName
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)

            } else if (field.fieldUid == PERSON_FIELD_UID_ATTENDANCE.toLong()) {
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)

            } else if (field.fieldUid == PERSON_FIELD_UID_CLASSES.toLong()) {
                thisValue = "Class Name ..."
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NAME_AND_PHONE_NUMBER.toLong()) {
                thisValue = thisPerson.fatherName + " (" + thisPerson.fatherNumber + ")"
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)

            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NAME_AND_PHONE_NUMBER.toLong()) {
                thisValue = thisPerson.motherName + " (" + thisPerson.motherNum + ")"
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NAME.toLong()) {
                thisValue = thisPerson.fatherName
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NAME.toLong()) {
                thisValue = thisPerson.motherName
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NUMBER.toLong()) {
                thisValue = thisPerson.fatherNumber
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NUMBER.toLong()) {
                thisValue = thisPerson.motherNum
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_ADDRESS.toLong()) {
                thisValue = thisPerson.address
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else if (field.fieldUid == PERSON_FIELD_UID_BIRTHDAY.toLong()) {
                thisValue = UMCalendarUtil.getPrettyDateFromLong(
                        thisPerson.dateOfBirth, currnetLocale)
                thisView.setField(field.fieldIndex, field.fieldUid,
                        PersonDetailViewField(field.fieldType,
                                field.labelMessageId, field.fieldIcon!!), thisValue!!)
            } else {//this is actually a custom field
                var messageLabel = 0
                var iconName: String? = null
                var fieldValue: String? = null
                if (valueMap!![field.fieldUid] != null) {
                    if (valueMap[field.fieldUid]!!.labelMessageId != 0) {
                        messageLabel = valueMap[field.fieldUid]!!.labelMessageId
                    }
                    if (valueMap[field.fieldUid]!!.fieldIcon != null) {
                        iconName = valueMap[field.fieldUid]!!.fieldIcon
                    }
                    if (valueMap[field.fieldUid]!!.customFieldValue!!.fieldValue != null) {
                        fieldValue = valueMap[field.fieldUid]!!
                                .customFieldValue!!.fieldValue
                    }
                }
                thisView.setField(
                        field.fieldIndex,
                        field.fieldUid,
                        PersonDetailViewField(
                                field.fieldType,
                                messageLabel,
                                iconName!!
                        ), fieldValue!!

                )
            }
        }
    }

    /**
     * Updates fields of Person w.r.t field Id given and the value. This method does NOT persist
     * the data.
     *
     * @param personToUpdate the Person object to update values for.
     * @param fieldcode The field Uid that needs to get updated
     * @param value The value to update the Person's field.
     * @return  The updated Person with the updated field.
     */
    private fun updateSansPersistPersonField(personToUpdate: Person?,
                                             fieldcode: Long, value: Any): Person {

        //Update Core fields
        if (fieldcode == PERSON_FIELD_UID_FIRST_NAMES.toLong()) {
            personToUpdate!!.firstNames = value as String

        } else if (fieldcode == PERSON_FIELD_UID_LAST_NAME.toLong()) {
            personToUpdate!!.lastName = value as String

        } else if (fieldcode == PERSON_FIELD_UID_FATHER_NAME.toLong()) {
            personToUpdate!!.fatherName = (value as String)

        } else if (fieldcode == PERSON_FIELD_UID_FATHER_NUMBER.toLong()) {
            personToUpdate!!.fatherNumber = (value as String)

        } else if (fieldcode == PERSON_FIELD_UID_MOTHER_NAME.toLong()) {
            personToUpdate!!.motherName = (value as String)

        } else if (fieldcode == PERSON_FIELD_UID_MOTHER_NUMBER.toLong()) {
            personToUpdate!!.motherName = (value as String)

        } else if (fieldcode == PERSON_FIELD_UID_BIRTHDAY.toLong()) {
            personToUpdate!!.dateOfBirth = (value as Long)

        } else if (fieldcode == PERSON_FIELD_UID_ADDRESS.toLong()) {
            personToUpdate!!.address = (value as String)

        } else {
            //This is actually a custom field. (old)
            GlobalScope.launch {
                val result = personCustomFieldValueDao.findCustomFieldByFieldAndPersonAsync(fieldcode, personToUpdate!!.personUid)
                if (result != null) {
                    result.fieldValue = (value.toString())
                    customFieldsToUpdate.add(result)
                } else {
                    //Create the custom field
                    val newCustomValue = PersonCustomFieldValue()
                    newCustomValue.personCustomFieldValuePersonUid = (personToUpdate.personUid)
                    newCustomValue.personCustomFieldValuePersonCustomFieldUid = (fieldcode)
                    personCustomFieldValueDao.insert(newCustomValue)
                    newCustomValue.fieldValue = (value.toString())
                    customFieldsToUpdate.add(newCustomValue)
                }
            }
        }

        return personToUpdate!!

    }

    /**
     * This method tells the View what to show. It will set every field item to the view.
     * The Live Data handler calls this method when the data (via Live data) is updated.
     *
     * @param person The person that needs to be displayed.
     */
    private fun handlePersonValueChanged(person: Person?) {
        //set the og person value
        if (mOriginalValuePerson == null)
            mOriginalValuePerson = person

        if (mUpdatedPerson == null || mUpdatedPerson != person) {
            //set fields on the view as they change and arrive.
            if (person != null) {
                setFieldsOnView(person, headersAndFields!!, view,
                        customFieldWithFieldValueMap)
                mUpdatedPerson = person
            }
        }
    }

    /**
     * Handles every field Edit (focus changed).
     *
     * @param fieldCode The field code that needs editing
     * @param value The new value of the field from the view
     */
    fun handleFieldEdited(fieldCode: Long, value: Any) {
        //TODO: Check this warning
        mUpdatedPerson = updateSansPersistPersonField(mUpdatedPerson, fieldCode, value)
    }

    /**
     * Click handler when Add new Class clicked on Classes section
     */
    fun handleClickAddNewClazz() {
        val args = HashMap<String, String>()
        args.put(ARG_PERSON_UID, personUid.toString())
        impl.go(PersonDetailEnrollClazzView.VIEW_NAME, args, context)
    }

    /**
     * Saves custom field values.
     * @param viewId
     * @param type
     * @param value
     */
    fun handleSaveCustomFieldValues(viewId: Int, type: Int, value: Any) {

        //Lookup viewId
        if (viewIdToCustomFieldUid.containsKey(viewId)) {
            val customFieldUid = viewIdToCustomFieldUid[viewId]!!

            var valueString: String? = null
            if (type == CustomField.FIELD_TYPE_TEXT) {
                valueString = value.toString()

            } else if (type == CustomField.FIELD_TYPE_DROPDOWN) {
                val spinnerSelection = value as Int
                val options = customFieldDropDownOptions[customFieldUid]
                //valueString = options.get(spinnerSelection);
                //or:
                valueString = spinnerSelection.toString()
            }

            if (valueString != null && !valueString.isEmpty()) {
                val finalValueString = valueString
                GlobalScope.launch {
                    val result = customFieldValueDao!!.findValueByCustomFieldUidAndEntityUid(customFieldUid, personUid)
                    val customFieldValue: CustomFieldValue?
                    if (result == null) {
                        customFieldValue = CustomFieldValue()
                        customFieldValue.customFieldValueEntityUid = personUid
                        customFieldValue.customFieldValueFieldUid = customFieldUid
                        customFieldValue.customFieldValueValue = finalValueString
                        customFieldValueDao!!.insert(customFieldValue)
                    } else {
                        customFieldValue = result
                        customFieldValue.customFieldValueValue = finalValueString
                        customFieldValueDao!!.update(customFieldValue)
                    }
                }
            }

        }
    }

    /**
     * Done click handler on the Edit / Enrollment page: Clicking done will persist and save it and
     * end the activity.
     *
     */
    fun handleClickDone() {
        mUpdatedPerson!!.active = true
        GlobalScope.launch {
            val result = personDao.updatePersonAsync(mUpdatedPerson!!, loggedInPersonUid!!)

            //Update the custom fields
            personCustomFieldValueDao.updateListAsync(customFieldsToUpdate)
            //Start of feed generation
            generateFeedsForPersonUpdate(repository, mUpdatedPerson!!)

            //Close the activity.
            view.finish()


        }

    }

    companion object {

        internal fun generateFeedsForPersonUpdate(repository: UmAppDatabase, mUpdatedPerson: Person) {
            //All edits trigger a feed
            val personClazzes = repository.clazzDao
                    .findAllClazzesByPersonUidAsList(mUpdatedPerson.personUid)

            val newFeedEntries = ArrayList<FeedEntry>()
            val updateFeedEntries = ArrayList<FeedEntry>()

            val feedLinkViewPerson = PersonDetailView.VIEW_NAME + "?" +
                    PersonDetailView.ARG_PERSON_UID + "=" +
                    mUpdatedPerson.personUid

            for (everyClazz in personClazzes) {
                val mneOfficerRole = repository.roleDao.findByNameSync(Role.ROLE_NAME_MNE)
                val mneofficers = repository.clazzDao.findPeopleWithRoleAssignedToClazz(
                        everyClazz.clazzUid, mneOfficerRole.roleUid)

                val admins = repository.personDao.findAllAdminsAsList()

                for (mne in mneofficers) {
                    val feedEntryUid = FeedEntryDao.generateFeedEntryHash(
                            mne.personUid, everyClazz.clazzUid,
                            ScheduledCheck.TYPE_CHECK_PERSON_PROFILE_UPDATED,
                            feedLinkViewPerson)

                    val thisEntry = FeedEntry(
                            feedEntryUid,
                            "Student details updated",
                            "Student " + mUpdatedPerson.firstNames
                                    + " " + mUpdatedPerson.lastName
                                    + " details updated",
                            feedLinkViewPerson,
                            everyClazz.clazzName!!,
                            mne.personUid
                    )
                    val existingEntry = repository.feedEntryDao.findByUid(feedEntryUid)

                    if (existingEntry == null) {
                        newFeedEntries.add(thisEntry)
                    } else {
                        updateFeedEntries.add(thisEntry)
                    }
                }

                for (admin in admins) {
                    val feedEntryUid = FeedEntryDao.generateFeedEntryHash(
                            admin.personUid, everyClazz.clazzUid,
                            ScheduledCheck.TYPE_CHECK_PERSON_PROFILE_UPDATED,
                            feedLinkViewPerson)

                    val thisEntry = FeedEntry(
                            feedEntryUid,
                            "Student details updated",
                            "Student " + mUpdatedPerson.firstNames
                                    + " " + mUpdatedPerson.lastName
                                    + " details updated",
                            feedLinkViewPerson,
                            everyClazz.clazzName!!,
                            admin.personUid
                    )

                    val existingEntry = repository.feedEntryDao.findByUid(feedEntryUid)

                    if (existingEntry == null) {
                        newFeedEntries.add(thisEntry)
                    } else {
                        updateFeedEntries.add(thisEntry)
                    }
                }
            }

            repository.feedEntryDao.insertList(newFeedEntries)
            repository.feedEntryDao.updateList(updateFeedEntries)

            //End of feed Generation
        }
    }


}
