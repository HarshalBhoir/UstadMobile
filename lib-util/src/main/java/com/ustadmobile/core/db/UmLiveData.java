package com.ustadmobile.core.db;

import com.ustadmobile.core.impl.UmLifecycleOwner;

/**
 * Created by mike on 1/13/18.
 */

public interface UmLiveData<T> {

    T getValue();

    void observe(UmLifecycleOwner controller, UmObserver<T> observer);

    void observeForever(UmObserver<T> observer);

    void removeObserver(UmObserver<T> observer);

}
