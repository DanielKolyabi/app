package ru.relabs.kurjer.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by ProOrange on 31.08.2018.
 */
@Entity(tableName = "firm_reject_reason")
data class FirmRejectReason(
        @PrimaryKey
        var id: Int,
        @ColumnInfo(name = "reason")
        var reason: String
)