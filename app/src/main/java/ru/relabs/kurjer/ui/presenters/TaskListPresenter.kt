package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.*
import org.joda.time.DateTime
import retrofit2.HttpException
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.data.models.ErrorUtils
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.ui.fragments.TaskListFragment
import ru.relabs.kurjer.ui.models.TaskListModel
import ru.relabs.kurjer.utils.activity
import ru.relabs.kurjer.utils.application
import ru.relabs.kurjer.utils.logError
import java.util.*

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {
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
        GlobalScope.launch(Dispatchers.Main) {
            val db = application().database

            //Load from network if available
            if (loadFromNetwork) {
                if (NetworkHelper.isNetworkAvailable(fragment.context)) {
                    var newTasks: List<TaskModel>?

                    try {
                        newTasks = withContext(Dispatchers.Default) {
                            withTimeout(7 * 60 * 1000L) {
                                loadTasksFromNetwork()
                            }
                        }
                    } catch (e: HttpException) {
                        e.printStackTrace()
                        val err = ErrorUtils.getError(e)
                        newTasks = null
                        if (err.code == 3) { //INVALID_DATE_TIME
                            fragment.activity()?.showError(err.message, object : ErrorButtonsListener {
                                override fun positiveListener() {
                                    fragment.activity()?.showLoginScreen()
                                }

                                override fun negativeListener() {}
                            })
                            return@launch
                        }
                        fragment.activity()
                            ?.showError("Задания не были обновлены. Возможна ошибка дат. Обратитесь к бригадиру.\nОшибка №${err.code}.")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        newTasks = null
                        fragment.activity()?.showError("Задания не были обновлены. Попробуйте обновить позже.")
                    }

                    if (newTasks != null) {
                        val token = (application().user as? UserModel.Authorized)?.token ?: ""

                        val mergeResult = PersistenceHelper.merge(
                            db,
                            newTasks,
                            {
                                db.sendQueryDao().insert(
                                    SendQueryItemEntity(
                                        0,
                                        BuildConfig.API_URL + "/api/v1/tasks/${it.id}/received?token=" + token,
                                        ""
                                    )
                                )

                                try {
                                    NetworkHelper.loadTaskRasterizeMap(it, fragment.context?.contentResolver)
                                } catch (e: Exception) {
                                    e.logError()
                                }
                            },
                            { oldTask, newTask ->
                                try {
                                    NetworkHelper.loadTaskRasterizeMap(newTask, fragment.context?.contentResolver)
                                } catch (e: Exception) {
                                    e.logError()
                                }
                            }
                        )
                        if (mergeResult.isTasksChanged) {
                            fragment.activity()?.showError("Задания были обновлены.")
                        } else if (mergeResult.isNewTasksAdded) {
                            fragment.activity()?.showError("Обновление прошло успешно.")
                        } else {
                            fragment.activity()?.showError("Нет новых заданий.")
                        }
                    }
                } else {
                    fragment.activity()?.showError("Отсутствует соединение с интернетом.\nНевозможно обновить данные.")
                }
            }

            //Delete all closed tasks that haven't ReportItems
            withContext(Dispatchers.Default) { PersistenceHelper.removeUnusedClosedTasks(db) }
            //Load from database
            val savedTasks = loadTasksFromDatabase(db)
            fragment.adapter.data.clear()
            fragment.adapter.data.addAll(savedTasks)
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
            fragment.scrollListToTarget()
        }
    }

    private suspend fun loadTasksFromNetwork(): List<TaskModel> {
        val app = application()
        val time = DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")
        val tasks = api.getTasks((app.user as UserModel.Authorized).token, time)
        return tasks.map { it.toTaskModel(app.deviceUUID) }
    }

    suspend fun loadTasksFromDatabase(db: AppDatabase): List<TaskListModel.Task> {
        val tasks = withContext(Dispatchers.Default) { db.taskDao().allOpened.map { it.toTaskModel(db) } }
        return tasks.filter { it.items.isNotEmpty() && it.canShowedByDate(Date()) }.map { TaskListModel.Task(it) }
    }
}