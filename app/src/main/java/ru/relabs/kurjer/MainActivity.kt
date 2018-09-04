package ru.relabs.kurjer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main)
        supportActionBar?.hide();
        back_button.setOnClickListener {
            onBackPressed()
        }
        showLoginScreen()
        supportFragmentManager.addOnBackStackChangedListener {
            val current = supportFragmentManager.findFragmentByTag("fragment")
            setNavigationRefreshVisible(current is TaskListFragment)
        }
    }

    fun showTaskListScreen() {
        navigateTo(TaskListFragment())
        changeTitle("Список заданий")
    }

    fun showLoginScreen() {
        navigateTo(LoginFragment())
        changeTitle("Авторизация")
    }

    fun showAddressListScreen(tasks: List<TaskModel>) {
        if (tasks.isEmpty()) {
            showError("Вы не выбрали ни одной задачи.")
        }
        navigateTo(AddressListFragment.newInstance(tasks), true)
        changeTitle("Список адресов")
    }

    fun showError(errorMessage: String) {
        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
    }

    fun showTaskDetailsScreen(task: TaskModel) {
        navigateTo(TaskDetailsFragment.newInstance(task), true)
        changeTitle("Детали задания")
    }

    fun showTaskItemExplanation(item: TaskItemModel) {
        navigateTo(TaskItemExplanationFragment.newInstance(item), true)
        changeTitle("Пояснения к заданию")

    }

    fun showTasksReportScreen(tasks: List<AddressListModel.TaskItem>) {

        navigateTo(ReportFragment.newInstance(
                tasks.map {
                    it.parentTask
                },
                tasks.map {
                    it.taskItem
                }), true)
    }

    fun changeTitle(title: String){
        top_app_bar.title.text = title
    }

    fun navigateTo(fragment: Fragment, isAddToBackStack: Boolean = false) {
        if (!isAddToBackStack) {
            clearBackStack()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment, "fragment")
            if (isAddToBackStack) {
                addToBackStack(null)
            }
        }.commit()

        val backVisible = when (fragment) {
            is LoginFragment -> false
            is TaskListFragment -> false
            else -> true
        }

        setNavigationBackVisible(backVisible)
        setNavigationRefreshVisible(fragment is TaskListFragment)
    }

    private fun setNavigationRefreshVisible(visible: Boolean) {
        refresh_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun setNavigationBackVisible(visible: Boolean) {
        back_button.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun clearBackStack() {

        val backStackEntryCount = this
                .supportFragmentManager
                .backStackEntryCount

        for (i in 0 until backStackEntryCount) {
            this.supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }

        setNavigationBackVisible(supportFragmentManager.backStackEntryCount > 1)

    }


}
