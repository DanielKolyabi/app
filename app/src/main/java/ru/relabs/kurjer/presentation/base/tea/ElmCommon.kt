package ru.relabs.kurjer.presentation.base.tea

import android.view.View
import com.github.terrakok.cicerone.Router
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.domain.storage.CurrentUserStorage
import ru.relabs.kurjer.domain.useCases.LoginUseCase
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.utils.extensions.showSnackbar

interface ErrorContext {
    var handleError: (DomainException) -> Unit
}

interface RouterContext {
    val router: Router
    val loginUseCase: LoginUseCase
}

class RouterContextMainImpl : RouterContext, KoinComponent {
    override val router: Router by inject()
    override val loginUseCase: LoginUseCase by inject()
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
                    c.loginUseCase.logout()
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

inline fun <Ctx, State> wrapInLoaders(
    crossinline loaderMessageFactory: (loaderDelta: Int) -> ElmMessage<Ctx, State>,
    crossinline body: suspend ChannelWrapper<Ctx, State>.(Ctx, State) -> Unit
): ElmEffect<Ctx, State> = { c, s ->
    wrapInLoaders(loaderMessageFactory) { body(c, s) }
}

suspend inline fun <Ctx, State, R> ChannelWrapper<Ctx, State>.wrapInLoaders(
    loaderMessageFactory: (loaderDelta: Int) -> ElmMessage<Ctx, State>,
    body: () -> R
): R {
    this.messages.send(loaderMessageFactory(1))
    val r = body()
    this.messages.send(loaderMessageFactory(-1))
    return r
}