package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import retrofit2.HttpException
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.network.models.ErrorUtils
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.NormalMergeStrategy
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.ui.fragments.TaskListFragment
import ru.relabs.kurjer.ui.models.TaskListModel

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {
    fun onTaskSelected(pos: Int) {
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        if (!isTaskCanSelected(task)) {
            (fragment.context as? MainActivity)?.showError("Вы должны ознакомиться с заданием.")
            return
        }

        task.apply {
            selected = !selected
        }
        fragment.adapter.notifyItemChanged(pos)
        updateStartButton()
    }

    fun onTaskClicked(pos: Int) {
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        (fragment.context as MainActivity).showTaskDetailsScreen(task)
    }

    fun onStartClicked() {
        (fragment.activity as? MainActivity)?.showAddressListScreen(
                fragment.adapter.data.filter {
                    (it is TaskListModel.Task) && it.task.selected
                }.map {
                    (it as TaskListModel.Task).task
                }
        )
    }

    fun updateStartButton() {
        fragment.setStartButtonActive(isStartAvailable())
    }

    fun isTaskCanSelected(task: TaskModel): Boolean {
        return task.state == TaskModel.EXAMINED || task.state == TaskModel.STARTED
    }

    fun isStartAvailable(): Boolean =
            fragment.adapter.data.any {
                if (it !is TaskListModel.Task) return@any false
                it.task.selected
            }

    fun loadTasks() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database

            //Load from network if available
//            if (NetworkHelper.isNetworkAvailable(fragment)) {
//                val newTasks = withContext(CommonPool){loadTasksFromNetwork()}
//                if(newTasks != null){
//                    //TODO: REWRITE FUCKING MERGE!!!
//                    if(PersistenceHelper.isMergeNeeded(db, newTasks)){
//                        PersistenceHelper.merge(db, newTasks, NormalMergeStrategy())
//                    }
//                }
//            } else {
//                fragment.activity().showError("Отсутствует соединение с интернетом.\nНевозможно обновить данные.")
//            }

            //Load from database
            val savedTasks = loadTasksFromDatabase(db)
            fragment.adapter.data.addAll(savedTasks)
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
            fragment.showListLoading(false)
        }
    }

    private suspend fun loadTasksFromNetwork(): List<TaskModel>? {
        try {
            val tasks = api.getTasks((fragment.application()!!.user as UserModel.Authorized).token).await()
            return tasks.map{it.toTaskModel()}
        } catch (e: HttpException) {
            e.printStackTrace()
            val err = ErrorUtils.getError(e)
            fragment.activity().showError("Ошибка №${err.code}.\n${err.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            fragment.activity().showError("Нет ответа от сервера.")
        }
        return null
    }

    suspend fun loadTasksFromDatabase(db: AppDatabase): List<TaskListModel.Task> {
        val tasks = withContext(CommonPool) { db.taskDao().all.map { it.toTaskModel(db) } }
        return tasks.filter { it.state != TaskModel.COMPLETED }.map { TaskListModel.Task(it) }
    }
}