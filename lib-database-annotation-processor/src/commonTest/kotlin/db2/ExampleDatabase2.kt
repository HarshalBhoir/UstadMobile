package db2

import androidx.room.Database
import com.ustadmobile.door.DoorDatabase

@Database(version = 1, entities = [ExampleEntity2::class])
abstract class ExampleDatabase2 : DoorDatabase(){

    abstract fun exampleDao2(): ExampleDao2

}