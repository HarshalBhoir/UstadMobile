package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.*
import com.ustadmobile.lib.db.entities.StatementEntity.Companion.TABLE_ID

@UmEntity(tableId = TABLE_ID)
@Entity
class StatementEntity {

    @UmPrimaryKey(autoGenerateSyncable = true)
    @PrimaryKey(autoGenerate = true)
    var statementUid: Long = 0

    var statementId: String? = null

    var personUid: Long = 0

    var verbUid: Long = 0

    var xObjectUid: Long = 0

    var subStatementActorUid: Long = 0

    var substatementVerbUid: Long = 0

    var subStatementObjectUid: Long = 0

    var agentUid: Long = 0

    var instructorUid: Long = 0

    var authorityUid: Long = 0

    var teamUid: Long = 0

    var resultCompletion: Boolean = false

    var resultSuccess: Byte = RESULT_UNSET

    var resultScoreScaled: Long = 0

    var resultScoreRaw: Long = 0

    var resultScoreMin: Long = 0

    var resultScoreMax: Long = 0

    var resultDuration: Long = 0

    var resultResponse: String? = null

    var timestamp: Long = 0

    var stored: Long = 0

    var contextRegistration: String? = null

    var contextPlatform: String? = null

    var contextStatementId: String? = null

    var fullStatement: String? = null

    @UmSyncMasterChangeSeqNum
    var statementMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var statementLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var statementLastChangedBy: Int = 0

    companion object {

        const val TABLE_ID = 60

        const val RESULT_UNSET = 0.toByte()

        const val RESULT_SUCCESS = 1.toByte()

        const val RESULT_FAILURE = 2.toByte()
    }
}
