package ru.relabs.kurjer.ui.presenters

import android.content.Intent
import android.net.Uri
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.*
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.AddressModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.helpers.TaskAddressSorter
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.utils.CustomLog
import ru.relabs.kurjer.utils.activity
import java.util.*


/**
 * Created by ProOrange on 09.08.2018.
 */
class AddressListPresenter(val fragment: AddressListFragment) {

    var sortingMethod = TaskAddressSorter.ALPHABETIC
    val tasks = mutableListOf<TaskModel>()

    fun changeSortingMethod(sorting: Int) {
        sortingMethod = sorting
        applySorting()
    }

    fun applySorting() {
        val items = mutableListOf<AddressListModel.TaskItem>()

        tasks.forEach { task ->
            items.addAll(
                    task.items.map {
                        AddressListModel.TaskItem(it, task)
                    }
            )
        }
        fragment.adapter.data.clear()
        if (tasks.size == 1) {
            fragment.adapter.data.add(AddressListModel.SortingItem(sortingMethod))
        }
        fragment.adapter.data.addAll(prepareTaskItemsForList(items))
        fragment.adapter.notifyDataSetChanged()
    }

    fun prepareTaskItemsForList(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel> {
        val sorted = if (sortingMethod == TaskAddressSorter.ALPHABETIC) {
            TaskAddressSorter.sortTaskItemsAlphabetic(taskItems)
        } else {
            TaskAddressSorter.sortTaskItemsStandart(taskItems)
        }

        return TaskAddressSorter.getAddressesWithTasksList(sorted)
    }

    private fun checkTasksIsClosed(db: AppDatabase) {
        if (tasks.isEmpty())
            return

        try {
            tasks.removeAll {
                if (isAllTaskItemsClosed(it)) {
                    PersistenceHelper.closeTask(db, it)
                    return@removeAll true
                }
                return@removeAll false
            }
        } catch (e: Exception) {
            CustomLog.writeToFile(CustomLog.getStacktraceAsString(e))
        }
    }

    private fun isAllTaskItemsClosed(task: TaskModel): Boolean {
        return !task.items.any { it.state != TaskItemModel.CLOSED }
    }

    fun onItemClicked(task: AddressListModel.TaskItem) {
        if (!task.parentTask.canShowedByDate(Date())) {
            fragment.activity()?.showError("Задание больше недоступно.", object : ErrorButtonsListener {
                override fun positiveListener() {
                    fragment?.activity()?.showTaskListScreen()
                }

                override fun negativeListener() {}
            })
        }
        fragment.activity()?.showTasksReportScreen(fragment.adapter.data.filter {
            (it is AddressListModel.TaskItem) && it.taskItem.address.id == task.taskItem.address.id
        }.map {
            it as AddressListModel.TaskItem
        }, task.parentTask.id)?.setTargetFragment(fragment, 1)
    }

    fun updateStates() {
        launch(UI) {
            val db = (fragment.activity?.application as? MyApplication)?.database
            db ?: run {
                return@launch
            }

            withContext(CommonPool) {
                tasks.forEach { task ->
                    task.items.map { item ->
                        val savedState = db.taskItemDao().getById(item.id)?.state
                        savedState ?: return@map
                        if (savedState != item.state) {
                            item.state = savedState
                        }
                    }
                }
            }

            applySorting()
            withContext(CommonPool) { checkTasksIsClosed(db) }
            if (tasks.size == 0) {
                (fragment.context as? MainActivity)?.showTaskListScreen(false)
            } else {
                fragment.scrollListToSavedPosition()
            }
        }
    }

    fun onItemMapClicked(task: TaskModel) {
        fragment.context ?: return
        val image = PathHelper.getTaskRasterizeMapFile(task)
        if (!image.exists()) {
            Toast.makeText(fragment.context, "Файл карты не найден.", Toast.LENGTH_SHORT).show()
            CustomLog.writeToFile("Для задания ${task.id} не удалось получить растровую карту. ${task.rastMapUrl}")
            return
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(Uri.fromFile(image), "image/*")
        startActivity(fragment.context!!, intent, null)
    }

    fun onDataChanged(changedTask: TaskModel, changedItem: TaskItemModel) {
        val taskIdx = tasks.indexOf(changedTask)
        if (taskIdx < 0) {
            return
        }
        val taskItemIdx = tasks[taskIdx].items.indexOf(changedItem)

        val items = tasks[taskIdx].items.toMutableList()
        items[taskItemIdx] = changedItem
        tasks[taskIdx].items = items
    }

    fun onMapAddressClicked(it: AddressModel) {
        fragment.targetAddress = it
    }
}

