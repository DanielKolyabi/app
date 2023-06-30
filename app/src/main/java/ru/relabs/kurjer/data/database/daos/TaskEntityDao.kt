package ru.relabs.kurjer.data.database.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.relabs.kurjer.data.database.entities.TaskEntity

/**
 * Created by ProOrange on 30.08.2018.
 */
@Dao
interface TaskEntityDao {

    @get:Query("SELECT * FROM tasks")
    val all: List<TaskEntity>

    @get:Query("SELECT * FROM tasks WHERE state = 4")
    val allClosed: List<TaskEntity>

    @get:Query("SELECT * FROM tasks WHERE state != 4")
    val allOpened: List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id in (:ids)")
    fun watchByIds(ids: List<Int>): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id in (:ids)")
    suspend fun getByIds(ids: List<Int>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE storage_id in (:ids)")
    suspend fun getTasksByStorageId(ids: List<Int>): List<TaskEntity>

    @Update
    suspend fun update(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(task: List<TaskEntity>)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Int)

}