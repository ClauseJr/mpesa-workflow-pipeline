package com.phabian.mpesa.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_sms",
    indices = [
        Index(
            value = ["smsId"],
            unique = true
        )
    ]
)
data class RawSmsEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val sender: String,

    val body: String,

    val receivedAt: Long,

    val smsId: String?,

    val parsed: Boolean,

    val parseStatus: ParseStatus
)