package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.core.db.UmProvider;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.OpdsEntry;
import com.ustadmobile.lib.db.entities.OpdsEntryWithRelations;

import java.util.List;

/**
 * Created by mike on 1/15/18.
 */

public abstract class OpdsEntryWithRelationsDao {

    @UmQuery("SELECT * from OpdsEntry WHERE url = :url")
    public abstract UmLiveData<OpdsEntryWithRelations> getEntryByUrl(String url, String entryUuid,
                                                             OpdsEntry.OpdsItemLoadCallback callback);

    public UmLiveData<OpdsEntryWithRelations> getEntryByUrl(String url, String entryUuid) {
        return getEntryByUrl(url, entryUuid, null);
    }

    public UmLiveData<OpdsEntryWithRelations> getEntryByUrl(String url) {
        return getEntryByUrl(url, null, null);
    }

    public abstract OpdsEntryWithRelations getEntryByUrlStatic(String url);

    @UmQuery("SELECT * from OpdsEntry INNER JOIN OpdsEntryToParentOpdsEntry on OpdsEntry.id = OpdsEntry.id WHERE OpdsEntryToParentOpdsEntry.parentEntry = :parentId")
    public abstract UmProvider<OpdsEntryWithRelations> getEntriesByParent(String parentId);

    @UmQuery("SELECT * from OpdsEntry INNER JOIN OpdsEntryToParentOpdsEntry on OpdsEntry.id = OpdsEntry.id WHERE OpdsEntryToParentOpdsEntry.parentEntry = :parentId")
    public abstract UmLiveData<List<OpdsEntryWithRelations>> getEntriesByParentAsList(String parentId);

    @UmQuery("SELECT * from OpdsEntry where id = :uuid")
    public abstract UmLiveData<OpdsEntryWithRelations> getEntryByUuid(String uuid);

    @UmQuery("SELECT id FROM OpdsEntry WHERE url = :url")
    public abstract String getUuidForEntryUrl(String url);

    @UmQuery("SELECT * FROM OpdsEntry " +
            "LEFT JOIN ContainerFileEntry on OpdsEntry.id = ContainerFileEntry.opdsEntryUuid " +
            "LEFT JOIN ContainerFile on ContainerFileEntry.containerFileId = ContainerFileEntry.id " +
            "WHERE ContainerFile.dirPath = :dir")
    public abstract UmLiveData<List<OpdsEntryWithRelations>> findEntriesByContainerFileDirectory(String dir);


}
