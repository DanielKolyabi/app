package ru.relabs.kurjer.presentation.host

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.relabs.kurjer.presentation.RootScreen
import ru.relabs.kurjer.utils.Left
import ru.relabs.kurjer.utils.Right
import ru.relabs.kurjer.utils.extensions.getFirebaseToken

object HostEffects {
    fun effectInit(restored: Boolean): HostEffect = { c, _ ->
        if (!restored) {
            if (c.repository.isAuthenticated()) {
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.Tasks)
                }

                withContext(Dispatchers.IO) {
                    when (val result = FirebaseInstanceId.getInstance().getFirebaseToken()) {
                        is Right -> Unit //TODO: Upload c.repository.updateMe(firebaseToken = result.value)
                        is Left -> FirebaseCrashlytics.getInstance().log("Can't get firebase token")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    c.router.newRootScreen(RootScreen.Login)
                }
            }
        }
    }

    fun effectCheckApiVersion(): HostEffect = { c, _ ->
        //TODO: Check app version
//        if (!ApiVersionUtils.checkApiVersion(c.repository)) {
//            c.showApiMismatchError()
//        }
    }

    fun effectLogout(): HostEffect = { c, s ->
        TODO("LogOut")
    }
}