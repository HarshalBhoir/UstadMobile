package db2

import android.arch.persistence.room.ColumnInfo
import androidx.room.PrimaryKey

open class ExampleEntity2(
        @PrimaryKey(autoGenerate = true)
        var uid: Long = 0L,
        var name: String = "",
        @ColumnInfo(index = true)
        var someNumber: Long = 0L) {


        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as ExampleEntity2

                if (uid != other.uid) return false
                if (name != other.name) return false
                if (someNumber != other.someNumber) return false

                return true
        }

        override fun hashCode(): Int {
                var result = uid.hashCode()
                result = 31 * result + name.hashCode()
                result = 31 * result + someNumber.hashCode()
                return result
        }
}