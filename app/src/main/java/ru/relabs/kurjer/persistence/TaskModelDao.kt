package ru.relabs.kurjer.persistence

import android.arch.persistence.room.*
import ru.relabs.kurjer.models.TaskModel

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskModelDao {

    @get:Query("SELECT * FROM tasks")
    val all: List<TaskModel>

    @Update
    fun update(task: TaskModel);
    @Insert
    fun insert(task: TaskModel);
    @Insert
    fun insertAll(task: List<TaskModel>);
    @Delete
    fun delete(task: TaskModel);
}