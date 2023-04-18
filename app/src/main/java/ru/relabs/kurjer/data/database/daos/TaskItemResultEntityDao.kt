package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemResultEntityDao {

    @get:Query("SELECT * FROM task_item_results")
    val all: List<TaskItemResultEntity>

    @Query("SELECT * FROM task_item_results WHERE id = :id")
    fun getById(id: Int): TaskItemResultEntity

    @Query("SELECT * FROM task_item_results WHERE task_item_id in (:ids)")
    suspend fun getByIds(ids: List<Int>): List<TaskItemResultEntity>

    @Query("SELECT * FROM task_item_results WHERE task_item_id = :id")
    fun getByTaskItemId(id: Int): TaskItemResultEntity?

    @Update
    fun update(task: TaskItemResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskItemResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(task: List<TaskItemResultEntity>)

    @Delete
    fun delete(task: TaskItemResultEntity)

    @Query("DELETE FROM task_item_results")
    suspend fun deleteAll()

    @Query("DELETE FROM task_item_results WHERE task_item_id in (:ids)")
    suspend fun deleteByTaskItemIds(ids: List<Int>)
}