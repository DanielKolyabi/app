package ru.relabs.kurjer.uiOld.presenters

import kotlinx.coroutines.*
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.repositories.DatabaseRepository
import ru.relabs.kurjer.domain.repositories.DeliveryRepository
import ru.relabs.kurjer.domain.repositories.SendQueryData
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.uiOld.fragments.TaskListFragment
import ru.relabs.kurjer.uiOld.models.TaskListModel
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.utils.log
import java.util.*

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(
    val fragment: TaskListFragment,
    val repository: DeliveryRepository,
    val database: AppDatabase,
    val authTokenStorage: AuthTokenStorage,
    val dbRep: DatabaseRepository
) {
    fun onTaskSelected(pos: Int) {
        if (pos < 0) {
            return
        }
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        if (!isTaskCanSelected(task)) {
            (fragment.context as? MainActivity)?.showError("Вы должны ознакомиться с заданием.")
            return
        }
        if (!task.isAvailableByDate(Date())) {
            (fragment.context as? MainActivity)?.showError("Дата начала распространения не наступила.")
            return
        }

        task.selected = !task.selected

        fragment.adapter.notifyItemChanged(pos)
        updateIntersectedTasks()
        updateStartButton()
    }

    fun updateIntersectedTasks() {
        val tasks = fragment.adapter.data

        val selectedTasks = tasks.filter { it is TaskListModel.Task && it.task.selected }
        val oldStates = tasks.map {
            if (it !is TaskListModel.Task) false
            else it.hasSelectedTasksWithSimilarAddress
        }

        val newStates = oldStates.map { false }.toMutableList()

        for (selectedTask in selectedTasks) {
            for ((i, task) in tasks.withIndex()) {
                if (task == selectedTask) continue
                if (task !is TaskListModel.Task) continue
                if (selectedTask !is TaskListModel.Task) continue
                if (newStates[i]) continue

                if (isTasksHasIntersectedAddresses(selectedTask.task, task.task)) {
                    newStates[i] = true
                }
            }
        }

        oldStates.forEachIndexed { i, state ->
            if (state != newStates[i]) {
                (fragment.adapter.data[i] as TaskListModel.Task).hasSelectedTasksWithSimilarAddress = newStates[i]
                fragment.adapter.notifyItemChanged(i)
            }
        }
    }

    fun isTasksHasIntersectedAddresses(task1: TaskModel, task2: TaskModel): Boolean {
        for (taskItem in task1.items) {
            if (task2.items.find { it.address.id == taskItem.address.id } != null) {
                return true
            }
        }
        return false
    }

    fun onTaskClicked(pos: Int) {
        if (pos < 0) {
            return
        }
        val task = (fragment.adapter.data[pos] as? TaskListModel.Task)?.task ?: return
        (fragment.context as MainActivity).showTaskDetailsScreen(task, pos)
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
        return (task.state and TaskModel.EXAMINED != 0) || (task.state and TaskModel.STARTED != 0)
    }

    fun isStartAvailable(): Boolean =
        fragment.adapter.data.any {
            if (it !is TaskListModel.Task) return@any false
            it.task.selected
        }

    fun loadTasks(loadFromNetwork: Boolean = false) {
        GlobalScope.launch {
            //Load from network if available
            if (loadFromNetwork) {
                if (NetworkHelper.isNetworkAvailable(fragment.context)) {
                    val newTasks = withContext(Dispatchers.Default) {
                        withTimeout(7 * 60 * 1000L) {
                            when (val tasks = repository.getTasks()) {
                                is Right -> tasks.value
                                is Left -> when (val e = tasks.value) {
                                    is DomainException.ApiException -> {
                                        if (e.error.code == 3) {
                                            fragment.activity()?.showError(e.error.message, object : ErrorButtonsListener {
                                                override fun positiveListener() {
//                                                    fragment.activity()?.showLoginScreen()
                                                }

                                                override fun negativeListener() {}
                                            })
                                        } else {
                                            fragment.activity()
                                                ?.showError("Задания не были обновлены. Возможна ошибка дат. Обратитесь к бригадиру.\nОшибка №${e.error.code}.")
                                        }
                                        null
                                    }
                                    else -> {
                                        fragment.activity()?.showError("Задания не были обновлены. Попробуйте обновить позже.")
                                        null
                                    }
                                }
                            }
                        }
                    }
                    if (newTasks != null) {
                        val token = authTokenStorage.getToken() ?: ""

//                        val mergeResult = PersistenceHelper.merge(
//                            database,
//                            newTasks,
//                            {
//                                dbRep.putSendQuery(SendQueryData.TaskReceived(it.id))
//
//                                try {
//                                    NetworkHelper.loadTaskRasterizeMap(it)
//                                } catch (e: Exception) {
//                                    e.log()
//                                }
//                            },
//                            { oldTask, newTask ->
//                                try {
//                                    NetworkHelper.loadTaskRasterizeMap(newTask)
//                                } catch (e: Exception) {
//                                    e.log()
//                                }
//                            },
//                            dbRep
//                        )
//                        if (mergeResult.isTasksChanged) {
//                            fragment.activity()?.showError("Задания были обновлены.")
//                        } else if (mergeResult.isNewTasksAdded) {
//                            fragment.activity()?.showError("Обновление прошло успешно.")
//                        } else {
//                            fragment.activity()?.showError("Нет новых заданий.")
//                        }
                    }
                } else {
                    fragment.activity()?.showError("Отсутствует соединение с интернетом.\nНевозможно обновить данные.")
                }
            }

            //Delete all closed tasks that haven't ReportItems
            withContext(Dispatchers.Default) { PersistenceHelper.removeUnusedClosedTasks(database) }
            //Load from database
            val savedTasks = loadTasksFromDatabase(database)
            fragment.adapter.data.clear()
            fragment.adapter.data.addAll(savedTasks)
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
            fragment.scrollListToTarget()
        }
    }

    suspend fun loadTasksFromDatabase(db: AppDatabase): List<TaskListModel.Task> {
        val tasks = withContext(Dispatchers.Default) { db.taskDao().allOpened.map { it.toTaskModel(db) } }
        return tasks.filter { it.items.isNotEmpty() && it.canShowedByDate(Date()) }.map { TaskListModel.Task(it) }
    }
}