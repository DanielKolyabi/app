package ru.relabs.kurjer.utils.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.relabs.kurjer.utils.Either

/**
 * Created by Daniil Kurchanov on 21.01.2020.
 */
suspend fun FirebaseMessaging.instanceIdSuspend() = suspendCancellableCoroutine<String> { cont ->
    token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            if(cont.isActive){
                cont.resumeWith(Result.failure(task.exception ?: RuntimeException("Firebase token retrieve failed. Task failed")))
            }
        }
        try {
            if(cont.isActive){
                when (val token = task.result) {
                    null -> cont.resumeWith(Result.failure(RuntimeException("Firebase token retrieve failed. Token is null")))
                    else -> cont.resumeWith(Result.success(token))
                }
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            if(cont.isActive){
                cont.resumeWith(Result.failure(e))
            }
        }
    }.addOnCanceledListener {
        if(cont.isActive) {
            cont.resumeWith(Result.failure(RuntimeException("Firebase token retrieve failed. Task canceled")))
        }
    }.addOnFailureListener {
        if(cont.isActive){
            cont.resumeWith(Result.failure(it))
        }
    }
}

suspend fun FirebaseMessaging.getFirebaseToken(): Either<Exception, String> {
    return Either.of {
        this.instanceIdSuspend()
    }
}