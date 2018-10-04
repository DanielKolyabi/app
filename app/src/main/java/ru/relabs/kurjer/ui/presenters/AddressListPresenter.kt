package ru.relabs.kurjer.ui.presenters

import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import ru.relabs.kurjer.files.PathHelper
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.UserModel
import ru.relabs.kurjer.persistence.AppDatabase
import ru.relabs.kurjer.persistence.PersistenceHelper
import ru.relabs.kurjer.persistence.entities.SendQueryItemEntity
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.helpers.TaskAddressSorter
import ru.relabs.kurjer.ui.models.AddressListModel
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.net.Uri
import ru.relabs.kurjer.*


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

    private fun checkTasksIsClosed(db: AppDatabase) {
        tasks.removeAll {
            if (isAllTaskItemsClosed(it)) {
                PersistenceHelper.closeTask(db, it)
                db.sendQueryDao().insert(
                        SendQueryItemEntity(0,
                                BuildConfig.API_URL + "/api/v1/tasks/${it.id}/completed?token=" + (fragment.application()!!.user as UserModel.Authorized).token,
                                "")
                )
                return@removeAll true
            }
            return@removeAll false
        }
    }

    fun prepareTaskItemsForList(taskItems: List<AddressListModel.TaskItem>): List<AddressListModel> {
        val sorted = if (sortingMethod == TaskAddressSorter.ALPHABETIC) {
            TaskAddressSorter.sortTaskItemsAlphabetic(taskItems)
        } else {
            TaskAddressSorter.sortTaskItemsStandart(taskItems)
        }

        return TaskAddressSorter.getAddressesWithTasksList(sorted)
    }

    private fun isAllTaskItemsClosed(task: TaskModel): Boolean {
        return !task.items.any { it.state != TaskItemModel.CLOSED }
    }

    fun onItemClicked(task: AddressListModel.TaskItem) {

        if(task.taskItem.state != TaskItemModel.CLOSED){
            (fragment.context as? MainActivity)?.showTasksReportScreen(fragment.adapter.data.filter {
                (it is AddressListModel.TaskItem) && it.taskItem.address.id == task.taskItem.address.id && it.taskItem.state != TaskItemModel.CLOSED
            }.map {
                it as AddressListModel.TaskItem
            }, task.parentTask.id)
        } else {
            (fragment.context as? MainActivity)?.showTasksReportScreen(listOf(task), task.parentTask.id)
        }
    }

    fun updateStates() {
        launch(UI) {
            val db = (fragment.activity!!.application as MyApplication).database

            withContext(CommonPool) {
                tasks.forEach { task ->
                    task.items.map { item ->
                        val savedState = db.taskItemDao().getById(item.id).state
                        if (savedState != item.state) {
                            item.state = savedState
                        }
                    }
                }
            }

            applySorting()

            withContext(CommonPool) { checkTasksIsClosed(db) }
            if (tasks.size == 0) {
                (fragment.context as MainActivity).showTaskListScreen(true)
            }
        }
    }

    fun onItemMapClicked(task: TaskModel) {
        fragment.context ?: return
        val image = PathHelper.getTaskRasterizeMapFile(task)
        if(!image.exists()){
            Toast.makeText(fragment.context, "Файл карты не найден.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.setDataAndType(Uri.fromFile(image), "image/*")
        startActivity(fragment.context!!, intent, null)
    }

}

