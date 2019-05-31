package db2

import androidx.room.*
import com.ustadmobile.door.DoorLiveData

@Dao
abstract class ExampleDao2 {

    @Insert
    abstract fun insertAndReturnId(entity: ExampleEntity2): Long

    @Insert
    abstract suspend fun insertAsync(entity: ExampleEntity2)

    @Insert
    abstract suspend fun insertAsyncAndGiveId(entity: ExampleEntity2) : Long

    @Insert
    abstract fun insertList(entityList: List<ExampleEntity2>)

    @Insert
    abstract fun insertOtherList(entityList: List<ExampleEntity2>)

    @Insert
    abstract fun insertAndReturnList(entityList: List<ExampleEntity2>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun replace(entityList: List<ExampleEntity2>)

    @Query("SELECT * FROM ExampleEntity2 WHERE uid = :uid")
    abstract fun findByUid(uid: Long): ExampleEntity2?

    @Query("SELECT * FROM ExampleEntity2 WHERE uid > :uid AND someNumber > :min")
    abstract suspend fun findLarge(uid: Long, min: Long): ExampleEntity2?

    @Query("SELECT name FROM ExampleEntity2 WHERE uid = :uid")
    abstract fun findNameByUid(uid: Long): String?

    @Query("SELECT ExampleEntity2.*, ExampleLinkEntity.* FROM " +
            " ExampleEntity2 LEFT JOIN ExampleLinkEntity ON ExampleEntity2.uid = ExampleLinkEntity.fkValue " +
            "WHERE ExampleEntity2.uid = :uid")
    abstract fun findByUidWithLinkEntity(uid: Long): ExampleEntity2WithExampleLinkEntity?

    @Query("SELECT * FROM ExampleEntity2 ORDER BY uid")
    abstract fun findAll(): List<ExampleEntity2>

    @Query("SELECT * FROM ExampleEntity2")
    abstract suspend fun findAllAsync(): List<ExampleEntity2WithExampleLinkEntity>

    @Update
    abstract fun updateSingleItem(entity: ExampleEntity2)

    @Update
    abstract fun updateSingleItemAndReturnCount(entity: ExampleEntity2): Int

    @Update
    abstract fun updateList(updateEntityList: List<ExampleEntity2>)

    @Query("SELECT * FROM ExampleEntity2")
    abstract fun findByMinUidLive(): DoorLiveData<List<ExampleEntity2>>


}