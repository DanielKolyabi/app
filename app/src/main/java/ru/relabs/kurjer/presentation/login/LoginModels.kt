package ru.relabs.kurjer.presentation.login

import android.os.Build
import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.relabs.kurjer.data.backup.DataBackupController
import ru.relabs.kurjer.data.models.auth.UserLogin
import ru.relabs.kurjer.domain.storage.AppInitialStorage
import ru.relabs.kurjer.domain.storage.SavedUserStorage
import ru.relabs.kurjer.domain.useCases.AppUpdateUseCase
import ru.relabs.kurjer.presentation.base.tea.ElmEffect
import ru.relabs.kurjer.presentation.base.tea.ElmMessage
import ru.relabs.kurjer.presentation.base.tea.ErrorContext
import ru.relabs.kurjer.presentation.base.tea.ErrorContextImpl
import ru.relabs.kurjer.presentation.base.tea.RouterContext
import ru.relabs.kurjer.presentation.base.tea.RouterContextMainImpl

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */

data class LoginState(
    val login: UserLogin = UserLogin(""),
    val password: String = "",
    val isPasswordRemembered: Boolean = true,
    val loaders: Int = 0,
    val dialogShowed: Boolean = false,
) {
    val permitted: StateFlow<Boolean>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MutableStateFlow(Environment.isExternalStorageManager())
        } else {
            MutableStateFlow(true)
        }
}

class LoginContext(val errorContext: ErrorContextImpl = ErrorContextImpl()) :
    ErrorContext by errorContext,
    RouterContext by RouterContextMainImpl(),
    KoinComponent {

    val updateUseCase: AppUpdateUseCase by inject()
    val savedUserStorage: SavedUserStorage by inject()
    val appInitialStorage: AppInitialStorage by inject()
    val dataBackupController: DataBackupController by inject()

    var showOfflineLoginOffer: () -> Unit = {}
    var showError: (id: Int) -> Unit = {}
    var showRestoreDialog: () -> Unit = {}
    var showSnackbar: suspend (Int) -> Unit = {}

}

typealias LoginMessage = ElmMessage<LoginContext, LoginState>
typealias LoginEffect = ElmEffect<LoginContext, LoginState>