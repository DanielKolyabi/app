package ru.relabs.kurjer.data.database.daos

import androidx.room.*
import ru.relabs.kurjer.data.database.entities.TaskItemResultEntranceEntity

/**
 * Created by ProOrange on 03.09.2018.
 */
@Dao
interface TaskItemResultEntranceEntityDao {

    @get:Query("SELECT * FROM task_item_result_entrances")
    val all: List<TaskItemResultEntranceEntity>

    @Query("SELECT * FROM task_item_result_entrances WHERE id = :id")
    fun getById(id: Int): TaskItemResultEntranceEntity

    @Query("SELECT * FROM task_item_result_entrances WHERE task_item_result_id = :id")
    fun getByTaskItemResultId(id: Int): List<TaskItemResultEntranceEntity>

    @Update
    fun update(task: TaskItemResultEntranceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: TaskItemResultEntranceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(task: List<TaskItemResultEntranceEntity>)

    @Delete
    fun delete(task: TaskItemResultEntranceEntity)
}