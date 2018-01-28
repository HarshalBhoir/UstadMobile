package com.ustadmobile.port.android.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.ustadmobile.core.db.dao.ContainerFileEntryDao;
import com.ustadmobile.lib.db.entities.ContainerFileEntry;

import java.util.List;

/**
 * Created by mike on 1/27/18.
 */

@Dao
public abstract class ContainerFileEntryDaoAndroid extends ContainerFileEntryDao{

    @Insert
    public abstract void insert(List<ContainerFileEntry> fileEntries);

    @Query("DELETE From OpdsEntry WHERE uuid in (Select opdsEntryUuid FROM ContainerFileEntry WHERE containerFileId = :containerFileId)")
    public abstract void deleteOpdsEntriesByContainerFile(int containerFileId);

    @Query("DELETE FROM ContainerFileEntry WHERE containerFileId = :containerFileId")
    public abstract void deleteContainerFileEntriesByContainerFile(int containerFileId);

    @Transaction
    public void deleteOpdsAndContainerFileEntriesByContainerFile(int containerFileId) {
        super.deleteOpdsAndContainerFileEntriesByContainerFile(containerFileId);
    }
}
