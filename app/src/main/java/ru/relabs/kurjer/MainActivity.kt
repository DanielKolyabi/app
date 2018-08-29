package ru.relabs.kurjer

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.fragments.LoginFragment
import ru.relabs.kurjer.ui.fragments.TaskDetailsFragment
import ru.relabs.kurjer.ui.fragments.TaskListFragment

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
        supportFragmentManager.addOnBackStackChangedListener{
            val current = supportFragmentManager.findFragmentByTag("fragment")
            setNavigationRefreshVisible(current is TaskListFragment)
        }
    }

    fun showTaskListScreen() {
        navigateTo(TaskListFragment())
        top_app_bar.title.text = "Список заданий"
    }

    fun showLoginScreen() {
        navigateTo(LoginFragment())
        top_app_bar.title.text = "Авторизация"
    }

    fun showAddressListScreen() {
        navigateTo(AddressListFragment(), true)
        top_app_bar.title.text = "Список адресов"
    }
    fun showTaskDetailsScreen(task: TaskModel) {
        navigateTo(TaskDetailsFragment.newInstance(task), true)
        top_app_bar.title.text = "Детали задания"
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

    private fun setNavigationRefreshVisible(visible: Boolean){
        refresh_button.visibility = if(visible) View.VISIBLE else View.GONE
    }
    private fun setNavigationBackVisible(visible: Boolean){
        back_button.visibility = if(visible) View.VISIBLE else View.GONE
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
