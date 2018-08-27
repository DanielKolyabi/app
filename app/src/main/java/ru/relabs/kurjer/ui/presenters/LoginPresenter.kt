package ru.relabs.kurjer.ui.presenters

import android.util.TypedValue
import kotlinx.android.synthetic.main.fragment_login.*
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.R
import ru.relabs.kurjer.ui.fragments.LoginFragment

/**
 * Created by ProOrange on 24.08.2018.
 */
class LoginPresenter(val fragment: LoginFragment) {

    private var isPasswordRemembered = false

    fun onLoad() {

    }

    fun onRememberPasswordClick() {
        isPasswordRemembered = !isPasswordRemembered
        fragment.setRememberPasswordEnabled(isPasswordRemembered)
    }

    fun onLoginClick(login: String, pwd: String) {
        //TODO: Get token
        (fragment.activity as? MainActivity)?.showTaskListScreen()
    }
}