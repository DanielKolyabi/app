package ru.relabs.kurjer.presentation

import android.graphics.Color
import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.androidx.FragmentScreen
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.addresses.AddressesFragment
import ru.relabs.kurjer.presentation.login.LoginFragment
import ru.relabs.kurjer.presentation.photoViewer.PhotoViewerFragment
import ru.relabs.kurjer.presentation.report.ReportFragment
import ru.relabs.kurjer.presentation.storageReport.StorageReportFragment
import ru.relabs.kurjer.presentation.storageList.StorageListFragment
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.presentation.taskDetails.TaskDetailsFragment
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.uiOld.fragments.TaskItemExplanationFragment
import ru.relabs.kurjer.uiOld.fragments.YandexMapFragment


object RootScreen {


    fun login() = FragmentScreen { LoginFragment.newInstance() }
    fun tasks(refreshTasks: Boolean) = FragmentScreen { TasksFragment.newInstance(refreshTasks) }
    fun addresses(tasks: List<Task>) = FragmentScreen { AddressesFragment.newInstance(tasks.map { it.id }) }

    fun <F> taskInfo(task: Task, parent: F) where F : Fragment, F : IExaminedConsumer = FragmentScreen {
        TaskDetailsFragment.newInstance(
            task,
            parent
        )
    }

    fun taskItemDetails(taskItem: TaskItem) = FragmentScreen { TaskItemExplanationFragment.newInstance(taskItem) }

    fun report(items: List<Pair<Task, TaskItem>>, selectedTaskItem: TaskItem) =
        FragmentScreen { ReportFragment.newInstance(items, selectedTaskItem) }

    fun imagePreview(imagePaths: List<String>) = FragmentScreen { PhotoViewerFragment.newInstance(imagePaths) }

    fun yandexMap(
        taskItems: List<TaskItem> = emptyList(),
        storages: List<YandexMapFragment.StorageLocation> = emptyList(),
        onAddressClicked: (Address) -> Unit
    ) = FragmentScreen {
        val addresses = taskItems
            .groupBy { it.address.id }
            .mapValues { entry ->
                entry.value.firstOrNull { (it.needPhoto || (it is TaskItem.Common && it.entrancesData.any { it.photoRequired })) && it.state != TaskItemState.CLOSED }
                    ?: (entry.value.firstOrNull { it.state != TaskItemState.CLOSED }
                        ?: entry.value.firstOrNull())
            }
            .mapNotNull { entry ->
                val value = entry.value
                if (value is TaskItem) {
                    val color = when {
                        value.state == TaskItemState.CLOSED ->
                            Color.GRAY
                        value.needPhoto || (value is TaskItem.Common && value.entrancesData.any { it.photoRequired }) ->
                            Color.parseColor("#EC3796")
                        else ->
                            Color.argb(255, 255, 165, 0)
                    }
                    YandexMapFragment.AddressWithColor(value.address, color)
                } else {
                    null
                }
            }

        //TODO: Strongly recommend to refactor
        YandexMapFragment.newInstance(addresses, storages).apply {
            this.onAddressClicked = onAddressClicked
        }
    }

    fun storageReportScreen(taskIds: List<TaskId>) = FragmentScreen { StorageReportFragment.newInstance(taskIds) }

    fun storageListScreen(taskIds: List<TaskId>) = FragmentScreen { StorageListFragment.newInstance(taskIds) }
}

