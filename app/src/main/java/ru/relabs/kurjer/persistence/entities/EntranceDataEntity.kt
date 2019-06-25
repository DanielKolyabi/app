package ru.relabs.kurjer.persistence.entities

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey
import ru.relabs.kurjer.models.EntranceDataModel

/**
 * Created by ProOrange on 24.06.2019.
 */
@Entity(tableName = "entrances_data", foreignKeys = [ForeignKey(
        entity = TaskItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["task_item_id"],
        onDelete = ForeignKey.CASCADE
)])
data class EntranceDataEntity(
        @PrimaryKey(autoGenerate = true)
        var id: Int,
        @ColumnInfo(name = "task_item_id")
        var taskItemId: Int,
        @ColumnInfo(name = "number")
        var number: Int,
        @ColumnInfo(name = "apartments_count")
        var apartmentsCount: Int,
        @ColumnInfo(name = "is_euro_boxes")
        var isEuroBoxes: Boolean,
        @ColumnInfo(name = "has_lookout")
        var hasLookout: Boolean,
        @ColumnInfo(name = "is_stacked")
        var isStacked: Boolean,
        @ColumnInfo(name = "is_refused")
        var isRefused: Boolean
) {
    fun toEntranceDataModel(): EntranceDataModel {
        return EntranceDataModel(number, apartmentsCount, isEuroBoxes, hasLookout, isStacked, isRefused)
    }
}