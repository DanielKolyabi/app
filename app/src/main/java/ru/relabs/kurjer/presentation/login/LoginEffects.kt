package ru.relabs.kurjer.presentation.login

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.common.DomainException
import ru.relabs.kurjer.domain.useCases.LoginResult
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.presentation.base.tea.CommonMessages
import ru.relabs.kurjer.presentation.base.tea.msgEffect
import ru.relabs.kurjer.presentation.base.tea.wrapInLoaders
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import timber.log.Timber

/**
 * Created by Daniil Kurchanov on 02.04.2020.
 */
object LoginEffects {

    fun effectInit(): LoginEffect = { c, s ->
        when (c.savedUserStorage.getCredentials()) {
            null -> {
                if (c.appInitialStorage.appFirstStarted && c.dataBackupController.backupExists) {
                    messages.send(LoginMessages.msgDialogShowed(true))
                    c.appInitialStorage.putInitialString()
                }
            }

            else -> {
                effectSetSavedCredentials()(c, s)
                if (c.appInitialStorage.appFirstStarted)
                    c.appInitialStorage.putInitialString()
            }
        }
    }

    fun effectStartCollect(): LoginEffect = { c, s ->
        coroutineScope {
            launch {
                c.connectivityProvider.connected.collect {
                    messages.send(LoginMessages.msgSetConnectivity(it))
                }
            }
        }
    }


    fun effectLoginCheck(isNetworkEnabled: Boolean): LoginEffect = { c, s ->
        withContext(Dispatchers.Main) {
            when (c.updateUseCase.isAppUpdated || c.updateUseCase.isUpdateUnavailable) {
                true -> when (isNetworkEnabled) {
                    true -> messages.send(msgEffect(effectLogin()))
                    false -> c.showError(R.string.login_need_network)
                }

                false -> c.showError(R.string.login_need_update)
            }
        }
    }

    private fun effectLogin(): LoginEffect = { c, s ->
        messages.send(LoginMessages.msgAddLoaders(1))
        when (val r = c.loginUseCase.login(s.login, s.password, s.isPasswordRemembered)) {
            is Right -> withContext(Dispatchers.Main) { c.router.replaceScreen(RootScreen.tasks(true)) }
            is Left -> when (val e = r.value) {
                is DomainException.ApiException -> messages.send(CommonMessages.msgError(r.value))
                else -> withContext(Dispatchers.Main) { c.showOfflineLoginOffer() }
            }
        }
        messages.send(LoginMessages.msgAddLoaders(-1))
    }

    fun effectLoginOffline(): LoginEffect = { c, s ->
        messages.send(LoginMessages.msgAddLoaders(1))
        when (c.loginUseCase.loginOffline(s.login, s.password)) {
            LoginResult.Success -> withContext(Dispatchers.Main) { c.router.replaceScreen(RootScreen.tasks(false)) }
            LoginResult.Wrong -> withContext(Dispatchers.Main) { c.showError(R.string.wrong_password_error) }
            LoginResult.Error -> withContext(Dispatchers.Main) { c.showError(R.string.login_offline_error) }
        }
        messages.send(LoginMessages.msgAddLoaders(-1))
    }

    fun effectRestoreData(): LoginEffect = wrapInLoaders({ LoginMessages.msgAddLoaders(it) }) { c, s ->
        withContext(Dispatchers.IO) {
            when (val r = c.dataBackupController.restore()) {
                is Right -> {
                    withContext(Dispatchers.Main) { c.showSnackbar(R.string.restore_succeeded) }
                    effectSetSavedCredentials()(c, s)
                }

                is Left -> {
                    Timber.e(r.value)
                    withContext(Dispatchers.Main) { c.showError(R.string.error_restore) }

                }
            }
        }
    }

    private fun effectSetSavedCredentials(): LoginEffect = { c, s ->
        val credentials = c.savedUserStorage.getCredentials()
        credentials?.let {
            messages.send(LoginMessages.msgLoginChanged(it.login))
        }
    }
}