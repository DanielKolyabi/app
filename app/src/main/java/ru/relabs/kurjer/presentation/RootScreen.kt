package ru.relabs.kurjer.presentation

import androidx.fragment.app.Fragment
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.presentation.login.LoginFragment
import ru.relabs.kurjer.presentation.tasks.TasksFragment
import ru.relabs.kurjer.uiOld.fragments.AddressListFragment
import ru.relabs.kurjer.uiOld.fragments.TaskDetailsFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen


sealed class RootScreen(protected val fabric: () -> Fragment) : SupportAppScreen() {

    override fun getFragment(): Fragment = fabric()

    object Login : RootScreen({ LoginFragment.newInstance() })
    class Tasks(refreshTasks: Boolean) : RootScreen({ TasksFragment.newInstance(refreshTasks) })
    class TaskInfo(task: Task) : RootScreen({ TaskDetailsFragment.newInstance(task) }) //TODO
    class Addresses(tasks: List<Task>) : RootScreen({ AddressListFragment.newInstance(tasks.map { it.id.id }) }) //TODO
}
