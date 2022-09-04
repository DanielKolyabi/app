package ru.relabs.kurjer.presentation

//import ru.relabs.kurjer.uiOld.fragments.TaskDetailsOldFragment
import android.graphics.Color
import androidx.fragment.app.Fragment
import ru.relabs.kurjer.domain.models.*
import ru.relabs.kurjer.presentation.addresses.AddressesFragment
import ru.relabs.kurjer.presentation.login.LoginFragment
import ru.relabs.kurjer.presentation.photoViewer.PhotoViewerFragment
import ru.relabs.kurjer.presentation.report.ReportFragment
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.presentation.taskDetails.TaskDetailsFragment
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.uiOld.fragments.TaskItemExplanationFragment
import ru.relabs.kurjer.uiOld.fragments.YandexMapFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen


sealed class RootScreen(protected val fabric: () -> Fragment) : SupportAppScreen() {

    override fun getFragment(): Fragment = fabric()

    object Login : RootScreen({ LoginFragment.newInstance() })
    class Tasks(refreshTasks: Boolean) : RootScreen({ TasksFragment.newInstance(refreshTasks) })
    class Addresses(tasks: List<Task>) : RootScreen({ AddressesFragment.newInstance(tasks.map { it.id }) })
    class TaskInfo<F>(task: Task, parent: F) :
        RootScreen({ TaskDetailsFragment.newInstance(task, parent) }) where F : Fragment, F : IExaminedConsumer

    class TaskItemDetails(taskItem: TaskItem) : RootScreen({ TaskItemExplanationFragment.newInstance(taskItem) })
    class Report(items: List<Pair<Task, TaskItem>>, selectedTaskItem: TaskItem) :
        RootScreen({ ReportFragment.newInstance(items, selectedTaskItem) })

    class ImagePreview(imagePaths: List<String>) : RootScreen({ PhotoViewerFragment.newInstance(imagePaths) })

    class YandexMap(
        taskItems: List<TaskItem>,
        storages: List<YandexMapFragment.StorageLocation> = emptyList(),
        onAddressClicked: (Address) -> Unit
    ) :
        RootScreen({
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
        })
}
