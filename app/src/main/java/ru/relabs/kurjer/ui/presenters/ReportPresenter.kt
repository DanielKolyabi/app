package ru.relabs.kurjer.ui.presenters

import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.models.ReportEntrancesListModel
import ru.relabs.kurjer.models.ReportPhotosListModel
import ru.relabs.kurjer.models.ReportTasksListModel
import ru.relabs.kurjer.ui.fragments.ReportFragment

class ReportPresenter(private val fragment: ReportFragment) {

    var currentTask = 0

    fun changeCurrentTask(taskNumber: Int) {
        fragment.setTaskListActiveTask(currentTask, false)
        fragment.setTaskListActiveTask(taskNumber, true)
        currentTask = taskNumber
        fragment.showHintText(fragment.taskItems[currentTask].notes)
        fillEntrancesAdapterData()
        fillPhotosAdapterData()

        (fragment.context as? MainActivity)?.changeTitle(fragment.taskItems[currentTask].address.name)
    }

    fun fillTasksAdapterData() {
        fragment.tasksListAdapter.data.addAll(fragment.tasks.mapIndexed { i, it ->
            ReportTasksListModel.TaskButton(it, i, i == 0)
        })
        fragment.tasksListAdapter.notifyDataSetChanged()
    }

    fun fillEntrancesAdapterData() {
        fragment.entrancesListAdapter.data.clear()
        if (fragment.tasks.size > 1) {
            fragment.setTaskListVisible(true)
        }
        fragment.entrancesListAdapter.data.addAll(fragment.taskItems[currentTask].entrances.map {
            ReportEntrancesListModel.Entrance(fragment.taskItems[currentTask], it)
        })
        fragment.entrancesListAdapter.notifyDataSetChanged()
    }

    fun fillPhotosAdapterData() {
        fragment.photosListAdapter.data.clear()
        fragment.photosListAdapter.data.add(ReportPhotosListModel.BlankPhoto)
        fragment.photosListAdapter.notifyDataSetChanged()
    }
}
