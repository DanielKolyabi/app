package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.EntranceDataEntity
import ru.relabs.kurjer.persistence.entities.TaskItemEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface EntranceDataEntityDao {

    @Query("SELECT * FROM entrances_data WHERE task_item_id = :taskItemId")
    fun getAllForTaskItem(taskItemId: Int): List<EntranceDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: EntranceDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(item: List<EntranceDataEntity>)

    @Delete
    fun delete(item: EntranceDataEntity)

    @Query("DELETE FROM entrances_data WHERE task_item_id = :taskItemId")
    fun deleteAllForTaskItem(taskItemId: Int)
}