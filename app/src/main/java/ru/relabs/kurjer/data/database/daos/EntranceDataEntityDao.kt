package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.EntranceDataEntity

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