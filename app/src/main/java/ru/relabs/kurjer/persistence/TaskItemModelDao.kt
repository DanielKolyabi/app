package ru.relabs.kurjer.persistence

import android.arch.persistence.room.*
import ru.relabs.kurjer.models.TaskItemModel

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskItemModelDao {

    @get:Query("SELECT * FROM task_items")
    val all: List<TaskItemModel>

    @Query("SELECT * FROM task_items WHERE task_id = :taskId")
    fun getAllForTask(taskId: Int): List<TaskItemModel>

    @Update
    fun update(item: TaskItemModel);

    @Insert
    fun insert(item: TaskItemModel);
    @Insert
    fun insertAll(item: List<TaskItemModel>);

    @Delete
    fun delete(item: TaskItemModel);
}