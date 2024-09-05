package ru.relabs.kurjer.data.models.tasks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey
import ru.relabs.kurjer.data.database.entities.storage.StorageReportEntity

@Entity(
    tableName = "task_item_report_relation",
    foreignKeys = [
        ForeignKey(
            entity = StorageReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_item_id"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = StorageReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["report_id"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index("task_item_id"),
        Index("report_id")
    ]
)
data class TaskItemReportRelation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "task_item_id")
    val taskItemId: Int,
    @ColumnInfo(name = "report_id")
    val reportId: Int
)