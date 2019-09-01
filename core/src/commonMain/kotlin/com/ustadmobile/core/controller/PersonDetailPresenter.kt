package com.ustadmobile.core.controller

import androidx.paging.DataSource
import com.ustadmobile.core.db.dao.CustomFieldDao
import com.ustadmobile.core.db.dao.CustomFieldValueDao
import com.ustadmobile.core.db.dao.CustomFieldValueOptionDao
import com.ustadmobile.core.db.dao.PersonPictureDao
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.UMCalendarUtil
import com.ustadmobile.core.view.*
import com.ustadmobile.core.view.PersonAuthDetailView.Companion.ARG_PERSONAUTH_PERSONUID
import com.ustadmobile.core.view.PersonDetailView.Companion.ARG_PERSON_UID
import com.ustadmobile.core.view.PersonPictureDialogView.Companion.ARG_PERSON_IMAGE_PATH
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
import com.ustadmobile.lib.db.entities.PersonField.Companion.FIELD_TYPE_TEXT
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch


/**
 * PersonDetail's Presenter - responsible for the logic of displaying all the details of a person
 * that is being displayed (for viewing). It also handles going to Edit page for this person; showing
 * all assigned classes for this person; setting ui bits for calling/texting parent, showing
 * attendance numbers, marking dropouts, showing profile image, etc.
 *
 */
class PersonDetailPresenter(context: Any, arguments: Map<String, String>?, view: PersonDetailView,
 val impl : UstadMobileSystemImpl = UstadMobileSystemImpl.instance) :
        UstadBaseController<PersonDetailView>(context, arguments!!, view) {

    private var mPerson: DoorLiveData<Person?>? = null

    private var presenterFields: List<PersonDetailPresenterField>? = null

    private var customFieldWithFieldValueMap: Map<Long, PersonCustomFieldWithPersonCustomFieldValue>? = null

    var personUid: Long = 0

    private var attendanceAverage: String? = null

    private var oneParentNumber = ""

    private val loggedInPersonUid: Long

    private var assignedClazzes: DataSource.Factory<Int, ClazzWithNumStudents>? = null

    private var personPictureDao: PersonPictureDao? = null

    private var currentPerson: Person? = null

    internal var repository = UmAccountManager.getRepositoryForActiveAccount(context)

    private val viewIdToCustomFieldUid: HashMap<Int, Long>

    private var customFieldDao: CustomFieldDao? = null
    private var customFieldValueDao: CustomFieldValueDao? = null
    private var optionDao: CustomFieldValueOptionDao? = null

    init {

        personUid = (arguments!!.get(ARG_PERSON_UID)!!.toString()).toLong()

        loggedInPersonUid = UmAccountManager.getActiveAccount(context)!!.personUid

        viewIdToCustomFieldUid = HashMap()
    }

    fun addToMap(viewId: Int, fieldId: Long) {
        viewIdToCustomFieldUid[viewId] = fieldId
    }

    /**
     * Presenter's overridden onCreate that: Gets the mPerson Live Data and observes it.
     *
     * @param savedState    The state
     */
    override fun onCreate(savedState: Map<String, String?>?) {
        super.onCreate(savedState)

        val personDao = repository.personDao
        val personDetailPresenterFieldDao = repository.personDetailPresenterFieldDao
        val clazzMemberDao = repository.clazzMemberDao

        customFieldDao = repository.customFieldDao
        customFieldValueDao = repository.customFieldValueDao
        optionDao = repository.customFieldValueOptionDao

        GlobalScope.launch {
            val thisPerson = personDao.findByUidAsync(personUid)
            if (thisPerson != null) {
                currentPerson = thisPerson
                if (thisPerson.fatherNumber != null && !thisPerson.fatherNumber!!.isEmpty()) {
                    oneParentNumber = thisPerson.fatherNumber!!
                } else if (thisPerson.motherNum != null && !thisPerson.motherNum!!.isEmpty
                        ()) {
                    oneParentNumber = thisPerson.motherNum!!
                }

                personPictureDao = repository.personPictureDao
                GlobalScope.launch {
                    val personPicture = personPictureDao!!.findByPersonUidAsync(thisPerson.personUid)
                    if (personPicture != null) {
                        //TODO: KMP
                        //view.updateImageOnView(personPictureDao!!.getAttachmentPath(personPicture.personPictureUid))
                    }
                }
            }
        }

        getAllPersonCustomFields()

        val thisP = this
        GlobalScope.launch {
            //Get all headers and fields
            val fields = personDetailPresenterFieldDao.findAllPersonDetailPresenterFieldsViewMode()
            val cleanedFields = ArrayList<PersonDetailPresenterField>()
            //Remove old custom fields
            val fieldsIterator = fields.iterator()
            while (fieldsIterator.hasNext()) {
                val field = fieldsIterator.next()
                val fieldIndex = field.fieldIndex
                if (fieldIndex == 19 || fieldIndex == 20 || fieldIndex == 21) {
                    //fieldsIterator.remove()
                }else{
                    cleanedFields.add(field)
                }
            }

            presenterFields = cleanedFields
            customFieldWithFieldValueMap = HashMap()


            //Get the attendance average for this person.
            GlobalScope.launch {
                val result = clazzMemberDao.getAverageAttendancePercentageByPersonUidAsync(personUid)
                if (result == null) {
                    attendanceAverage = "N/A"
                } else {
                    attendanceAverage = (result * 100).toString() + "%"
                }

                //Get person live data and observe
                mPerson = personDao.findByUidLive(personUid)

                view.runOnUiThread(Runnable {
                    mPerson!!.observe(thisP, thisP::handlePersonDataChanged)
                })

            }
        }

        checkPermissions()
    }

    /**
     * Getting custom fields (new way)
     */
    private fun getAllPersonCustomFields() {
        //0. Clear all added custom fields on view.
        view.runOnUiThread(Runnable{ view.clearAllCustomFields() })

        //1. Get all custom fields
        GlobalScope.launch {
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
                    val finalValueString = arrayOf<String>(valueString as String)
                    view.runOnUiThread(Runnable{
                        if (finalValueString[0].isEmpty()) {
                            finalValueString[0] = "-"
                        }
                        //view.addCustomFieldText(c, finalValueString);
                        view.addComponent(finalValueString[0], c.customFieldName!!)
                    })

                } else if (c.customFieldType == CustomField.FIELD_TYPE_DROPDOWN) {
                    if (result2 != null) {
                        try {
                            valueSelection = result2.customFieldValueValue!!.toInt()
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
                    view.runOnUiThread(Runnable{
                        view.addComponent(finalValueString, c.customFieldName!!)

                    })

                }

            }
        }
    }

    /**
     * Check permission for logged in user and enable/disable view components
     */
    fun checkPermissions() {
        val clazzDao = repository.clazzDao
        GlobalScope.launch {
            val result = clazzDao.personHasPermission(loggedInPersonUid, Role.PERMISSION_PERSON_UPDATE)
            view.showFAB(result!!)
        }

        val personDao = repository.personDao
        GlobalScope.launch {
            val result2 = personDao.personHasPermission(loggedInPersonUid, personUid,Role.PERMISSION_PERSON_PICTURE_UPDATE)
            view.showUpdateImageButton(result2!!)
        }

        GlobalScope.launch {
            val result = clazzDao.personHasPermission(loggedInPersonUid, Role.PERMISSION_PERSON_INSERT)
            view.showDropout(result!!)
            view.showEnrollInClass(result)
        }
    }

    /**
     * Compresses the image and updates it on on the view.
     * @param imageFile The image file object
     */
    fun handleCompressedImage(imageFilePath: String) {
        val personPictureDao = repository.personPictureDao
        val personPicture = PersonPicture()
        personPicture.personPicturePersonUid = personUid
        personPicture.picTimestamp = UMCalendarUtil.getDateInMilliPlusDays(0)

        val personDao = repository.personDao

        GlobalScope.launch {
            val personPictureUid = personPictureDao.insertAsync(personPicture)
            //TODO: KMP attachment
            //personPictureDao.setAttachmentFromTmpFile(personPictureUid, imageFile)

            //Update person and generate feeds for person
            val result = personDao.updateAsync(currentPerson!!)
            PersonEditPresenter.generateFeedsForPersonUpdate(repository, currentPerson!!)

            //TODO: KMP attachment
            //view.updateImageOnView(personPictureDao.getAttachmentPath(personPictureUid))
        }

    }


    /**
     * Generates the all class list with assignation for the person being displayed.
     */
    fun generateAssignedClazzesLiveData() {
        val clazzDao = repository.clazzDao

        assignedClazzes = clazzDao.findAllClazzesByPersonUid(personUid)

        setClazzListOnView()
    }

    /**
     * Sets the Class List provider of ClazzNumWithStudents type to the view.
     */
    private fun setClazzListOnView() {
        view.setClazzListProvider(assignedClazzes!!)
    }

    /**
     * This method tells the View what to show. It will set every field item to the view.
     * @param person The person that needs to be displayed.
     */
    private fun handlePersonDataChanged(person: Person?) {

        //TODO: KMP Locale ?
        //val currentLocale = Locale(impl.getLocale(context))
        val currentLocale = null

        view.clearAllFields()

        if (person == null) {
            return
        }

        for (field in presenterFields!!) {

            var thisValue: String? = ""

            if (field.fieldType == FIELD_TYPE_HEADER) {
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_HEADER,
                        field.headerMessageId, null), field.headerMessageId)
                continue
            }

            if (field.fieldUid == PERSON_FIELD_UID_FULL_NAME.toLong()) {
                if (person.firstNames != null && person.lastName != null)
                    thisValue = person.firstNames + " " + person.lastName
                else
                    thisValue = ""
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_FIRST_NAMES.toLong()) {
                thisValue = person.firstNames
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_LAST_NAME.toLong()) {
                thisValue = person.lastName
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_ATTENDANCE.toLong()) {
                if (attendanceAverage != null) {
                    thisValue = attendanceAverage
                }
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_CLASSES.toLong()) {
                thisValue = "Class Name ..."
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon!!), thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NAME_AND_PHONE_NUMBER.toLong()) {
                if (person.fatherNumber == null) {
                    thisValue = person.fatherName
                } else {
                    thisValue = person.fatherName + " (" + person.fatherNumber + ")"
                }
                //Also tell the view that we need to add call and text buttons for the number

                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, person.fatherNumber, field.fieldIcon),
                        thisValue)

            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NAME_AND_PHONE_NUMBER.toLong()) {
                if (person.motherNum == null) {
                    thisValue = person.motherName
                } else {
                    thisValue = person.motherName + " (" + person.motherNum + ")"
                }

                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, person.motherNum, field.fieldIcon),
                        thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NAME.toLong()) {
                thisValue = person.fatherName
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NAME.toLong()) {
                thisValue = person.motherName
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_FATHER_NUMBER.toLong()) {
                if (person.fatherName != null) {
                    if (!person.fatherName!!.isEmpty()) {
                        oneParentNumber = person.fatherNumber!!
                    }
                }
                thisValue = person.fatherNumber
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_MOTHER_NUMBER.toLong()) {
                if (person.motherNum != null) {
                    if (!person.motherNum!!.isEmpty()) {
                        oneParentNumber = person.motherNum!!
                    }
                }
                thisValue = person.motherNum
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_ADDRESS.toLong()) {
                thisValue = person.address
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else if (field.fieldUid == PERSON_FIELD_UID_BIRTHDAY.toLong()) {
                thisValue = UMCalendarUtil.getPrettyDateFromLong(
                        person.dateOfBirth, currentLocale)
                view.setField(field.fieldIndex, PersonDetailViewField(FIELD_TYPE_TEXT,
                        field.labelMessageId, field.fieldIcon), thisValue)
            } else {//this is actually a custom field

                val cf = customFieldWithFieldValueMap!![field.fieldUid]
                var cfLabelMessageId = 0
                var cfFieldIcon: String? = ""
                var cfValue: Any? = null

                if (cf != null) {
                    if (cf.labelMessageId != 0) {
                        cfLabelMessageId = cf.labelMessageId
                    }
                    if (cf.fieldIcon != null) {
                        cfFieldIcon = cf.fieldIcon
                    }
                    if (cf.customFieldValue != null) {
                        if (cf.customFieldValue!!.fieldValue !=
                                null) {
                            cfValue = cf.customFieldValue!!.fieldValue
                        }
                    }
                }

                view.setField(
                        field.fieldIndex,
                        PersonDetailViewField(
                                field.fieldType,
                                cfLabelMessageId,
                                cfFieldIcon!!
                        ),
                        cfValue!!
                )
            }
        }
    }

    /**
     * This method is called upon clicking the edit button for that person.
     * This wil take us to PersonDetailEdit
     */
    fun handleClickEdit() {

        view.finish()
        val args = HashMap<String, String>()
        args.put(ARG_PERSON_UID, personUid.toString())
        impl.go(PersonEditView.VIEW_NAME, args, view.viewContext)
    }

    /**
     * Handles call parent button - calls the method that calls the parent (open platform native
     * call UI)
     */
    fun handleClickCallParent() {
        if (!oneParentNumber.isEmpty()) {
            handleClickCall(oneParentNumber)
        }
    }

    /**
     * Handles text parent button - calls the method that texts the parent (opens platform native
     * text UI)
     */
    fun handleClickTextParent() {
        if (!oneParentNumber.isEmpty()) {
            handleClickText(oneParentNumber)
        }
    }

    /**
     * Opens person picture (almost) fullscreen dialog
     * @param imagePath The full path of the image
     */
    fun openPictureDialog(imagePath: String) {
        val args = HashMap<String, String>()
        args.put(ARG_PERSON_IMAGE_PATH, imagePath)
        args.put(ARG_PERSON_UID, personUid.toString())
        impl.go(PersonPictureDialogView.VIEW_NAME, args, context)
    }

    /**
     * Handles what happens when Enroll in Class is clicked at the top common big buttons
     * - opens the View that shows the list of classes with their enrollment status ie:
     * PersonDetailEnrollClazz
     */
    fun handleClickEnrollInClass(){
        val args = HashMap<String, String>()
        args.put(ARG_PERSON_UID, personUid.toString())

        impl.go(PersonDetailEnrollClazzView.VIEW_NAME, args, context)
    }

    fun handleClickRecordDropout() {
        val args = HashMap<String, String>()
        args.put(ARG_PERSON_UID, personUid.toString())

        impl.go(RecordDropoutDialogView.VIEW_NAME, args, context)
    }

    /**
     * Handler to what happens when call button pressed on an entry (usually to call a person)
     *
     * @param number The phone number
     */
    fun handleClickCall(number: String) {
        view.handleClickCall(number)
    }

    /**
     * Handler to what happens when text / sms button pressed on an entry (usually to text a
     * person / parent)
     *
     * @param number The phone number
     */
    fun handleClickText(number: String) {
        view.handleClickText(number)
    }

    fun goToUpdateUsernamePassword() {
        val args = HashMap<String, String>()
        args.put(ARG_PERSONAUTH_PERSONUID, currentPerson!!.personUid.toString())
        impl.go(PersonAuthDetailView.VIEW_NAME, args, context)
    }
}
