package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.TaskEntity
import ru.relabs.kurjer.domain.models.TaskId

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
    fun getByIds(ids: List<TaskId>): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getById(id: Int): TaskEntity?

    @Update
    fun update(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(task: List<TaskEntity>)

    @Delete
    fun delete(task: TaskEntity)
}