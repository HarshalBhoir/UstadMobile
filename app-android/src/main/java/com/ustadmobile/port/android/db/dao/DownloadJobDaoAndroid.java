package com.ustadmobile.port.android.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.ustadmobile.core.db.dao.DownloadJobDao;
import com.ustadmobile.lib.db.entities.DownloadJob;
import com.ustadmobile.lib.db.entities.DownloadJobWithRelations;

/**
 * Created by mike on 2/2/18.
 */
@Dao
public abstract class DownloadJobDaoAndroid extends DownloadJobDao {

    @Override
    @Insert
    public abstract long insert(DownloadJob job);

    @Override
    @Query("Update DownloadJob SET status = :status, timeRequested = :timeRequested WHERE id = :id")
    public abstract long queueDownload(int id, int status, long timeRequested);

    @Override
    @Query("SELECT * FROM DownloadJob WHERE status > 0 AND status <= 10 ORDER BY timeRequested LIMIT 1")
    protected abstract DownloadJobWithRelations findNextDownloadJob();

    @Override
    @Query("UPDATE DownloadJob SET status = :status WHERE id = :jobId")
    public abstract long updateJobStatus(int jobId, int status);

    @Transaction
    public DownloadJobWithRelations findNextDownloadJobAndSetStartingStatus(){
        return super.findNextDownloadJobAndSetStartingStatus();
    }

}
