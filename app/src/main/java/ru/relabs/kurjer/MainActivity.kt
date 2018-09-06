package ru.relabs.kurjer

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.*
import ru.relabs.kurjer.ui.models.AddressListModel

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

        if (!(application as MyApplication).enableLocationListening()) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showError("Необходимо разрешить приложению получать ваше местоположение.", object : ErrorButtonsListener {
                    override fun positiveListener() {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    }

                    override fun negativeListener() {}
                }, true)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showError("Необходимо разрешить приложению получать ваше местоположение.", object : ErrorButtonsListener {
                    override fun positiveListener() {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
                    }

                    override fun negativeListener() {
                        onBackPressed()
                    }
                })
            } else {
                if (!(application as MyApplication).enableLocationListening()) {
                    showError("Невозможно включить получение расположения.")
                }
            }
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

    fun showError(errorMessage: String, listener: ErrorButtonsListener? = null, forceHideNegativeButton: Boolean = false) {
        val builder = AlertDialog.Builder(this)
                .setMessage(errorMessage)
                .setPositiveButton("Ок") { _, _ -> listener?.positiveListener() }
        if (!forceHideNegativeButton) {
            listener?.let { builder.setNegativeButton("Отмена") { _, _ -> listener?.negativeListener() } }
        }
        builder.show()
    }

    fun showTaskDetailsScreen(task: TaskModel) {
        navigateTo(TaskDetailsFragment.newInstance(task), true)
        changeTitle("Детали задания")
    }

    fun showTaskItemExplanation(item: TaskItemModel) {
        navigateTo(TaskItemExplanationFragment.newInstance(item), true)
        changeTitle("Пояснения к заданию")

    }

    fun showTasksReportScreen(tasks: List<AddressListModel.TaskItem>, selectedTaskId: Int) {

        navigateTo(ReportFragment.newInstance(
                tasks.map {
                    it.parentTask
                },
                tasks.map {
                    it.taskItem
                },
                selectedTaskId
        ), true)
    }

    fun changeTitle(title: String) {
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

interface ErrorButtonsListener {
    fun positiveListener()
    fun negativeListener()
}
