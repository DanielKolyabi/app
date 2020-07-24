package ru.relabs.kurjer.presentation.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.CommonMessages
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object LoginEffects {

    fun effectInit(): LoginEffect = { c, s ->

    }

    fun effectLogin(): LoginEffect = { c, s ->
        messages.send(LoginMessages.msgAddLoaders(1))
        when (val r = c.loginUseCase.login(s.login, s.password)) {
            is Right -> withContext(Dispatchers.Main) { c.router.replaceScreen(RootScreen.Tasks) }
            is Left -> when (val e = r.value) {
                is DomainException.ApiException -> messages.send(CommonMessages.msgError(r.value))
                else -> withContext(Dispatchers.Main) { c.showOfflineLoginOffer() }
            }
        }
        messages.send(LoginMessages.msgAddLoaders(-1))
    }

    fun effectLoginOffline(): LoginEffect = { c, s ->
        messages.send(LoginMessages.msgAddLoaders(1))
        when (c.loginUseCase.loginOffline()) {
            null -> withContext(Dispatchers.Main) { c.showOfflineLoginError() }
            else -> withContext(Dispatchers.Main) { c.router.replaceScreen(RootScreen.Tasks) }
        }
        messages.send(LoginMessages.msgAddLoaders(-1))
    }
}