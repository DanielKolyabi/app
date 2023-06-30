package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemResultEntityDao {

    @get:Query("SELECT * FROM task_item_results")
    val all: List<TaskItemResultEntity>

    @Query("SELECT * FROM task_item_results WHERE id = :id")
    suspend fun getById(id: Int): TaskItemResultEntity

    @Query("SELECT * FROM task_item_results WHERE task_item_id in (:ids)")
    suspend fun getByIds(ids: List<Int>): List<TaskItemResultEntity>

    @Query("SELECT * FROM task_item_results WHERE task_item_id = :id")
    suspend fun getByTaskItemId(id: Int): TaskItemResultEntity?

    @Update
    suspend fun update(task: TaskItemResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItemResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(task: List<TaskItemResultEntity>)

    @Delete
    suspend fun delete(task: TaskItemResultEntity)

    @Query("DELETE FROM task_item_results")
    suspend fun deleteAll()

    @Query("DELETE FROM task_item_results WHERE task_item_id in (:ids)")
    suspend fun deleteByTaskItemIds(ids: List<Int>)
}