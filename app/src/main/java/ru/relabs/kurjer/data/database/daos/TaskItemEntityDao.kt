package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ru.relabs.kurjer.data.database.entities.TaskItemEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskItemEntityDao {

    @get:Query("SELECT * FROM task_items")
    val all: List<TaskItemEntity>

    @Query("SELECT * FROM task_items WHERE id = :id")
    suspend fun getById(id: Int): TaskItemEntity?

    @Query("SELECT * FROM task_items WHERE task_id = :taskId")
    suspend fun getAllForTask(taskId: Int): List<TaskItemEntity>

    @Query("SELECT * FROM task_items WHERE address_id = :id")
    suspend fun getByAddressId(id: Int): List<TaskItemEntity>

    @Update
    suspend fun update(item: TaskItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TaskItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(item: List<TaskItemEntity>)

    @Delete
    suspend fun delete(item: TaskItemEntity)

    @Query("DELETE FROM task_items WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM task_items")
    suspend fun deleteAll()

    @Query("DELETE FROM task_items WHERE task_id = :id")
    suspend fun deleteByTaskId(id: Int)
}