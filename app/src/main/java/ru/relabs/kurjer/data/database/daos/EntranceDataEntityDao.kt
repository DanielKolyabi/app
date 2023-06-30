package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.relabs.kurjer.data.database.entities.EntranceDataEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface EntranceDataEntityDao {

    @Query("SELECT * FROM entrances_data WHERE task_item_id = :taskItemId")
    suspend fun getAllForTaskItem(taskItemId: Int): List<EntranceDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EntranceDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(item: List<EntranceDataEntity>)

    @Delete
    suspend fun delete(item: EntranceDataEntity)

    @Query("DELETE FROM entrances_data WHERE task_item_id = :taskItemId")
    suspend fun deleteAllForTaskItem(taskItemId: Int)

    @Query("DELETE FROM entrances_data")
    suspend fun deleteAll()
}