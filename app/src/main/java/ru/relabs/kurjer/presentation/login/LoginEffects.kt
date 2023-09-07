package ru.relabs.kurjer.presentation.login

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.models.common.DomainException
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
                Log.d("zxc", "Backup exists = ${c.dataBackupController.backupExists}")
                if (c.appInitialStorage.appFirstStarted && c.dataBackupController.backupExists) {
                    messages.send(LoginMessages.msgDialogShowed(true))
                    effectShowRestoreDialog()(c, s)
                    c.appInitialStorage.putInitialString()
                }
            }

            else -> {
                Log.d("zxc", "Backup exists = ${c.dataBackupController.backupExists}")
                effectSetSavedCredentials()(c, s)
                if (c.appInitialStorage.appFirstStarted)
                    c.appInitialStorage.putInitialString()
            }
        }
    }

    fun effectShowRestoreDialog(): LoginEffect = { c, s ->
        Log.d("LoginEffects", "effectShowRestoreDialog")
        c.showRestoreDialog()
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
        when (c.loginUseCase.loginOffline()) {
            null -> withContext(Dispatchers.Main) { c.showError(R.string.login_offline_error) }
            else -> withContext(Dispatchers.Main) { c.router.replaceScreen(RootScreen.tasks(false)) }
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
            messages.send(LoginMessages.msgPasswordChanged(it.password))
        }
    }
}