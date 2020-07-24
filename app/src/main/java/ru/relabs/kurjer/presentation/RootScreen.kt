package ru.relabs.kurjer.presentation

import androidx.fragment.app.Fragment
import ru.relabs.kurjer.presentation.login.LoginFragment
import ru.terrakok.cicerone.Screen
import ru.terrakok.cicerone.android.support.SupportAppScreen


sealed class RootScreen(protected val fabric: () -> Fragment) : SupportAppScreen() {

    override fun getFragment(): Fragment = fabric()

    object Login : RootScreen({ LoginFragment() })
    object Tasks : RootScreen({ LoginFragment() }) //TODO: Change to real tasks fragment
}
