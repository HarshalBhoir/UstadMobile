package com.ustadmobile.port.android.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.ustadmobile.core.db.dao.ContainerFileEntryDao;
import com.ustadmobile.core.impl.UmCallback;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.ContainerFile;
import com.ustadmobile.lib.db.entities.ContainerFileEntry;
import com.ustadmobile.lib.db.entities.ContainerFileEntryWithContainerFile;

import java.util.List;
import java.util.concurrent.ExecutorService;

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

    //TODO: rework this to avoid writing file paths etc for remote devices
    @Override
    @Query("SELECT * FROM ContainerFileEntry WHERE containerEntryId IN (:entryIds)")
    public abstract List<ContainerFileEntry> findContainerFileEntriesByEntryIds(String[] entryIds);

    @Override
    @Query("SELECT * FROM ContainerFileEntry " +
            "LEFT JOIN ContainerFile ON ContainerFileEntry.containerFileId = ContainerFile.id " +
            "WHERE ContainerFileEntry.containerEntryId = :entryId")
    public abstract ContainerFileEntryWithContainerFile findContainerFileEntryWithContainerFileByEntryId(String entryId);

    @Override
    @Query("SELECT * FROM ContainerFileEntry " +
                  "LEFT JOIN ContainerFile ON ContainerFileEntry.containerFileId = ContainerFile.id " +
                  "WHERE ContainerFileEntry.containerEntryId = :entryId")
    public abstract List<ContainerFileEntryWithContainerFile> findContainerFileEntriesWithContainerFileByEntryId(String entryId);

    @Override
    @Query("SELECT opdsEntryUuid FROM ContainerFileEntry WHERE containerFileId = :containerFileId")
    protected abstract List<String> findOpdsEntryUuidsByContainerFileId(int containerFileId);

    @Override
    @Query("SELECT containerEntryId FROM ContainerFileEntry WHERE containerFileId = :containerFileId")
    public abstract List<String> findEntryIdsByContainerFile(int containerFileId);

    @Override
    @Query("DELETE from ContainerFileEntry Where containerFileId = :containerFileId")
    protected abstract void deleteByContainerFileId(int containerFileId);
}
