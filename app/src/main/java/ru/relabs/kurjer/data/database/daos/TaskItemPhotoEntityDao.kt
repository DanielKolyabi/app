package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.TaskItemPhotoEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemPhotoEntityDao {

    @get:Query("SELECT * FROM task_item_photos")
    val all: List<TaskItemPhotoEntity>

    @Query("SELECT * FROM task_item_photos WHERE id = :id")
    suspend fun getById(id: Int): TaskItemPhotoEntity

    @Query("SELECT * FROM task_item_photos WHERE task_item_id = :id")
    suspend fun getByTaskItemId(id: Int): List<TaskItemPhotoEntity>

    @Update
    suspend fun update(task: TaskItemPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskItemPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(task: List<TaskItemPhotoEntity>)

    @Delete
    suspend fun delete(task: TaskItemPhotoEntity)

    @Query("DELETE FROM task_item_photos WHERE task_item_id = :taskItemId")
    suspend fun deleteByTaskItemId(taskItemId: Int)

    @Query("DELETE FROM task_item_photos WHERE id = :photoId")
    suspend fun deleteById(photoId: Int)

    @Query("DELETE FROM task_item_photos")
    suspend fun deleteAll()
}