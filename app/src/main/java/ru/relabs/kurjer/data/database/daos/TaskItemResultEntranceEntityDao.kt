package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemResultEntranceEntityDao {

    @get:Query("SELECT * FROM task_item_result_entrances")
    val all: List<TaskItemResultEntranceEntity>

    @Query("SELECT * FROM task_item_result_entrances WHERE id = :id")
    suspend fun getById(id: Int): TaskItemResultEntranceEntity

    @Query("SELECT * FROM task_item_result_entrances WHERE task_item_result_id = :id")
    suspend fun getByTaskItemResultId(id: Int): List<TaskItemResultEntranceEntity>

    @Update
    suspend fun update(task: TaskItemResultEntranceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItemResultEntranceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(task: List<TaskItemResultEntranceEntity>)

    @Delete
    suspend fun delete(task: TaskItemResultEntranceEntity)

    @Query("DELETE FROM task_item_result_entrances")
    suspend fun deleteAll()

    @Query("DELETE FROM task_item_result_entrances WHERE task_item_result_id in (:ids)")
    suspend fun deleteByTaskItemResultIds(ids: List<Int>)
}