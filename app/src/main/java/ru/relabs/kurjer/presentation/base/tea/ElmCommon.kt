package ru.relabs.kurjer.presentation.base.tea

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.utils.extensions.showSnackbar
import ru.terrakok.cicerone.Router

interface ErrorContext {
    var handleError: (DomainException) -> Unit
}

interface RouterContext {
    val router: Router
    val authTokenStorage: AuthTokenStorage
    val currentUserStorage: CurrentUserStorage
}

class RouterContextMainImpl : RouterContext, KoinComponent {
    override val router: Router by inject()
    override val authTokenStorage: AuthTokenStorage by inject()
    override val currentUserStorage: CurrentUserStorage by inject()
}

class ErrorContextImpl : ErrorContext {

    fun attach(view: View) {
        handleError = { e ->
            view.post {
                when (e) {
                    is DomainException.ApiException -> showSnackbar(view, e.error.message)
                    is DomainException.UnknownException -> showSnackbar(
                        view, view.resources.getString(R.string.unknown_network_error)
                    )
                }
            }
        }
    }

    fun detach() {
        handleError = {}
    }

    override var handleError: (DomainException) -> Unit = {}
}


object CommonMessages {

    fun <C, S> msgError(error: DomainException): ElmMessage<C, S> where C : ErrorContext, C : RouterContext {
        return when (error) {
            is DomainException.ApiException -> when (error.error.code) {
                401 -> msgEffect { c, _ ->
                    c.authTokenStorage.resetToken()
                    c.currentUserStorage.resetCurrentUserLogin()
                    withContext(Dispatchers.Main) {
                        c.router.newRootScreen(RootScreen.Login)
                    }
                }
                else -> msgEffect { c, _ -> c.handleError(error) }
            }
            is DomainException.CanceledException -> msgEffect { c, _ -> c.handleError(error) }
            is DomainException.UnknownException -> msgEffect { c, _ -> c.handleError(error) }
        }
    }
}