package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.TaskEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskEntityDao {

    @get:Query("SELECT * FROM tasks")
    val all: List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: Int): TaskEntity?

    @Update
    fun update(task: TaskEntity);

    @Insert
    fun insert(task: TaskEntity);

    @Insert
    fun insertAll(task: List<TaskEntity>);

    @Delete
    fun delete(task: TaskEntity);
}