package ru.relabs.kurjer.presentation.login

import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.msgEffects
import ru.relabs.kurjer.presentation.base.tea.msgState

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

object LoginMessages {
    fun msgInit(): LoginMessage = msgEffects(
        { it },
        { listOf(LoginEffects.effectInit()) }
    )

    fun msgLoginChanged(login: UserLogin): LoginMessage =
        msgState { it.copy(login = login) }

    fun msgPasswordChanged(password: String): LoginMessage =
        msgState { it.copy(password = password) }

    fun msgRememberChanged(): LoginMessage =
        msgState { it.copy(isPasswordRemembered = !it.isPasswordRemembered) }

    fun msgLoginClicked(isNetworkEnabled: Boolean): LoginMessage =
        msgEffect(LoginEffects.effectLoginCheck(isNetworkEnabled))

    fun msgLoginOffline(): LoginMessage =
        msgEffect(LoginEffects.effectLoginOffline())

    fun msgAddLoaders(i: Int): LoginMessage =
        msgState { it.copy(loaders = it.loaders + i) }

    fun msgRestoreData(): LoginMessage =
        msgEffect(LoginEffects.effectRestoreData())

}