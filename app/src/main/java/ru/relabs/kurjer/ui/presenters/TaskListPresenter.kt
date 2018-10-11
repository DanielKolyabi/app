package ru.relabs.kurjer.ui.presenters

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import retrofit2.HttpException
import ru.relabs.kurjer.ErrorButtonsListener
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.application
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.network.DeliveryServerAPI.api
import ru.relabs.kurjer.network.NetworkHelper
import ru.relabs.kurjer.network.models.ErrorUtils
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.ui.fragments.TaskListFragment
import ru.relabs.kurjer.ui.models.TaskListModel
import java.text.SimpleDateFormat
import java.util.*

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
        if (!task.isAvailableByDate(Date())){
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
        return (task.state and TaskModel.EXAMINED != 0) || task.state == TaskModel.STARTED
    }

    fun isStartAvailable(): Boolean =
            fragment.adapter.data.any {
                if (it !is TaskListModel.Task) return@any false
                it.task.selected
            }

    fun loadTasks(loadFromNetwork: Boolean = false) {
        launch(UI) {
            fragment.application() ?: return@launch
            val db = fragment.application()!!.database

            //Load from network if available
            if (loadFromNetwork) {
                if (NetworkHelper.isNetworkAvailable(fragment.context)) {
                    var newTasks: List<TaskModel>

                    try {
                        newTasks = withContext(CommonPool) { loadTasksFromNetwork() }
                    } catch (e: HttpException) {
                        e.printStackTrace()
                        val err = ErrorUtils.getError(e)
                        newTasks = listOf()
                        if(err.code == 3){ //INVALID_DATE_TIME
                            fragment.activity()?.showError(err.message, object: ErrorButtonsListener{
                                override fun positiveListener() {
                                    fragment.activity()?.showLoginScreen()
                                }
                                override fun negativeListener() {}
                            })
                            return@launch
                        }
                        fragment.activity()?.showError("Задания не были обновлены. Попробуйте обновить в ручную.\nОшибка №${err.code}.")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        newTasks = listOf()
                        fragment.activity()?.showError("Задания не были обновлены. Попробуйте обновить в ручную.")
                    }

                    if (newTasks.isNotEmpty()) {
                        val mergeResult = PersistenceHelper.merge(
                                db,
                                newTasks,
                                {
                                    try {
                                        NetworkHelper.loadTaskRasterizeMap(it, fragment.context?.contentResolver)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                { oldTask, newTask ->
                                    if (oldTask.rastMapUrl != newTask.rastMapUrl) {
                                        try {
                                            NetworkHelper.loadTaskRasterizeMap(newTask, fragment.context?.contentResolver)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
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
            withContext(CommonPool) { PersistenceHelper.removeUnusedClosedTasks(db) }
            //Load from database
            val savedTasks = loadTasksFromDatabase(db)
            fragment.adapter.data.clear()
            fragment.adapter.data.addAll(savedTasks)
            fragment.adapter.notifyDataSetChanged()
            updateStartButton()
        }
    }

    private suspend fun loadTasksFromNetwork(): List<TaskModel> {
        val time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(Date())
        val tasks = api.getTasks((fragment.application()!!.user as UserModel.Authorized).token, time).await()
        return tasks.map { it.toTaskModel() }
    }

    suspend fun loadTasksFromDatabase(db: AppDatabase): List<TaskListModel.Task> {
        val tasks = withContext(CommonPool) { db.taskDao().allOpened.map { it.toTaskModel(db) } }
        return tasks.filter { it.items.isNotEmpty() }.map { TaskListModel.Task(it) }
    }
}