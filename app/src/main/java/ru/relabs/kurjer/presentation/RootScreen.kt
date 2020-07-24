package ru.relabs.kurjer.presentation

import androidx.fragment.app.Fragment
import ru.relabs.kurjer.uiOld.fragments.LoginFragment
import ru.terrakok.cicerone.android.support.SupportAppScreen


sealed class RootScreen(protected val fabric: () -> Fragment) : SupportAppScreen() {

    override fun getFragment(): Fragment = fabric()

    object Login : RootScreen({ LoginFragment() })
}
