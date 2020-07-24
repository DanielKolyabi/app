package ru.relabs.kurjer.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by ProOrange on 06.09.2018.
 */

@Entity(tableName = "send_query")
data class SendQueryItemEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        var url: String,
        var post_data: String
)