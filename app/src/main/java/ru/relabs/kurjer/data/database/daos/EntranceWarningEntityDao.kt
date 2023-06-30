package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.relabs.kurjer.data.database.entities.EntranceWarningEntity

@Dao
interface EntranceWarningEntityDao {
    @Query("SELECT * FROM entrance_warnings WHERE entrance_number = :entranceNumber AND task_item_id = :taskItemId")
    suspend fun getWarning(entranceNumber: Int, taskItemId: Int): EntranceWarningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: EntranceWarningEntity)

    @Query("DELETE FROM entrance_warnings WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: Int)
}