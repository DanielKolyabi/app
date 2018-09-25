package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

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