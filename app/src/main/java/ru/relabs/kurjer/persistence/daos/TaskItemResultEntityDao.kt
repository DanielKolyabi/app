package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.TaskItemResultEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemResultEntityDao {

    @get:Query("SELECT * FROM task_item_results")
    val all: List<TaskItemResultEntity>

    @Query("SELECT * FROM task_item_results WHERE id = :id")
    fun getById(id: Int): TaskItemResultEntity

    @Query("SELECT * FROM task_item_results WHERE task_item_id = :id")
    fun getByTaskItemId(id: Int): TaskItemResultEntity?

    @Update
    fun update(task: TaskItemResultEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskItemResultEntity): Long;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(task: List<TaskItemResultEntity>);

    @Delete
    fun delete(task: TaskItemResultEntity);
}