package ru.relabs.kurjer.persistence.daos

import android.arch.persistence.room.*
import ru.relabs.kurjer.persistence.entities.TaskEntity
import ru.relabs.kurjer.persistence.entities.TaskItemPhotoEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemPhotoEntityDao {

    @get:Query("SELECT * FROM task_item_photos")
    val all: List<TaskItemPhotoEntity>

    @Query("SELECT * FROM task_item_photos WHERE id = :id")
    fun getById(id: Int): TaskItemPhotoEntity

    @Query("SELECT * FROM task_item_photos WHERE task_item_id = :id")
    fun getByTaskItemId(id: Int): List<TaskItemPhotoEntity>

    @Update
    fun update(task: TaskItemPhotoEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskItemPhotoEntity): Long;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(task: List<TaskItemPhotoEntity>);

    @Delete
    fun delete(task: TaskItemPhotoEntity);
}