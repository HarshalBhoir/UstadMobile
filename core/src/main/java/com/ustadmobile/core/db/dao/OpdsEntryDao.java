package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.OpdsEntry;

import java.util.List;

/**
 * Created by mike on 1/15/18.
 */

public abstract class OpdsEntryDao {

    public abstract long insert(OpdsEntry entry);

    public abstract void insertList(List<OpdsEntry> entries);


    @UmQuery("SELECT (COUNT(*) > 0) From OpdsEntry WHERE id = :entryId")
    public abstract UmLiveData<Boolean> isEntryPresent(String entryId);

}
