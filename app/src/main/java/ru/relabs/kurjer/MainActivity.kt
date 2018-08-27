package ru.relabs.kurjer

import android.support.v7.app.AppCompatActivity
import android.support.v4.app.Fragment
import android.os.Bundle
import android.view.View
import android.view.Window
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import ru.relabs.kurjer.ui.fragments.AddressListFragment
import ru.relabs.kurjer.ui.fragments.LoginFragment
import ru.relabs.kurjer.ui.fragments.TaskListFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main)
        supportActionBar?.hide();

        showLoginScreen()
    }

    fun showTaskListScreen(){
        navigateTo(TaskListFragment())
        top_app_bar.title.text = "Список заданий"
    }

    fun showLoginScreen(){
        navigateTo(LoginFragment())
        top_app_bar.title.text = "Авторизация"
    }

    fun navigateTo(fragment: Fragment){
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment, "fragment")
        }.commit()

        refresh_button.visibility = if(fragment is TaskListFragment) View.VISIBLE else View.GONE
    }
}
