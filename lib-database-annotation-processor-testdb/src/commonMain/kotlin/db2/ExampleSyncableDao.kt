package db2

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.door.DoorLiveData

@Dao
abstract class ExampleSyncableDao {

    @Insert
    abstract fun insert(syncableEntity: ExampleSyncableEntity): Long

    @Insert
    abstract suspend fun insertAsync(syncableEntity: ExampleSyncableEntity): Long

    @Query("SELECT * FROM ExampleSyncableEntity")
    abstract fun findAll(): List<ExampleSyncableEntity>

    @Query("SELECT * FROM ExampleSyncableEntity WHERE esUid = :uid")
    abstract fun findByUid(uid: Long): ExampleSyncableEntity?

    @Query("SELECT ExampleSyncableEntity.*, OtherSyncableEntity.* FROM " +
            "ExampleSyncableEntity LEFT JOIN OtherSyncableEntity ON ExampleSyncableEntity.esUid = OtherSyncableEntity.otherFk")
    abstract fun findAllWithOtherByUid(): List<ExampleSyncableEntityWithOtherSyncableEntity>

    @Query("SELECT * FROM ExampleSyncableEntity")
    abstract fun findAllLive(): DoorLiveData<List<ExampleSyncableEntity>>

    @Query("SELECT * FROM ExampleSyncableEntity")
    abstract fun findAllDataSource(): DataSource.Factory<Int, ExampleSyncableEntity>

    @Query("UPDATE ExampleSyncableEntity SET esNumber = :newNumber," +
            "esLcb = (SELECT nodeClientId FROM SyncNode LIMIT 1) " +
            "WHERE " +
            "esUid = :uid")
    abstract fun updateNumberByUid(uid: Long, newNumber: Long)

}