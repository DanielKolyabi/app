package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.TaskItemEntity
import ru.relabs.kurjer.persistence.entities.TaskItemResultEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskItemEntityDao {

    @get:Query("SELECT * FROM task_items")
    val all: List<TaskItemEntity>

    @Query("SELECT * FROM task_items WHERE id = :id")
    fun getById(id: Int): TaskItemEntity

    @Query("SELECT * FROM task_items WHERE task_id = :taskId")
    fun getAllForTask(taskId: Int): List<TaskItemEntity>

    @Query("SELECT * FROM task_items WHERE address_id = :id")
    fun getByAddressId(id: Int): List<TaskItemEntity>

    @Update
    fun update(item: TaskItemEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: TaskItemEntity): Long;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(item: List<TaskItemEntity>);

    @Delete
    fun delete(item: TaskItemEntity);

}