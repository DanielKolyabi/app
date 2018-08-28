package ru.relabs.kurjer.ui.presenters

import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.ui.fragments.TaskListFragment

/**
 * Created by ProOrange on 27.08.2018.
 */
class TaskListPresenter(val fragment: TaskListFragment) {

    fun onStartClicked(){
        (fragment.activity as? MainActivity)?.showAddressListScreen()
    }
}