package ru.relabs.kurjer.presentation

import androidx.fragment.app.Fragment
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.presentation.addresses.AddressesFragment
import ru.relabs.kurjer.presentation.login.LoginFragment
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.presentation.taskDetails.TaskDetailsFragment
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.uiOld.fragments.TaskDetailsOldFragment
import ru.relabs.kurjer.uiOld.fragments.TaskItemExplanationFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen


sealed class RootScreen(protected val fabric: () -> Fragment) : SupportAppScreen() {

    override fun getFragment(): Fragment = fabric()

    object Login : RootScreen({ LoginFragment.newInstance() })
    class Tasks(refreshTasks: Boolean) : RootScreen({ TasksFragment.newInstance(refreshTasks) })
    class Addresses(tasks: List<Task>) : RootScreen({ AddressesFragment.newInstance(tasks.map { it.id }) })
    class TaskInfo<F>(task: Task, parent: F) :
        RootScreen({ TaskDetailsFragment.newInstance(task, parent) }) where F : Fragment, F : IExaminedConsumer

    class TaskItemDetails(taskItem: TaskItem) : RootScreen({ TaskItemExplanationFragment.newInstance(taskItem) }) //TODO
}
