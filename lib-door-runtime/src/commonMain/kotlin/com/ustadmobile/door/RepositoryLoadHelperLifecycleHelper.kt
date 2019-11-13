package com.ustadmobile.door

/**
 * This expect/actual is used by RepositoryLoadHelper to monitor the lifecycle of anything which is
 * observing it's related LiveData.  This allows the RepositoryLoadHelper to load automatically when
 * there are pending requests where the results of which are being actively observed, and avoid
 * attempting to reload data that is not being actively observed.
 *
 * The RepositoryLoadHelper must call addObserver to start observing the lifecycleOwner and
 * removeObserver to stop observing the given lifecycleOwner.
 */
expect class RepositoryLoadHelperLifecycleHelper(loadHelper: RepositoryLoadHelper<*>, lifecycleOwner: DoorLifecycleOwner) {

    /**
     * Function to run when the given lifecycleOwner becomes active
     */
    var onActive: (() -> Unit)?

    /**
     * Function to run when the given lifecycleOwner becomes inActive
     */
    var onInactive: (() -> Unit)?

    /**
     * Function to call to start actively observing the lifecycleOwner
     */
    fun addObserver()

    fun removeObserver()

}