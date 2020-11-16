package ru.relabs.kurjer.utils.extensions

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.suspendCancellableCoroutine
import ru.relabs.kurjer.utils.Either

/**
 * Created by Daniil Kurchanov on 21.01.2020.
 */
suspend fun FirebaseInstanceId.instanceIdSuspend() = suspendCancellableCoroutine<String> { cont ->
    instanceId.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            cont.resumeWith(Result.failure(task.exception ?: RuntimeException("Firebase token retrieve failed. Task failed")))
        }
        try {
            when (val token = task.result?.token) {
                null -> cont.resumeWith(Result.failure(RuntimeException("Firebase token retrieve failed. Token is null")))
                else -> cont.resumeWith(Result.success(token))
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            cont.resumeWith(Result.failure(e))
        }
    }.addOnCanceledListener {
        cont.resumeWith(Result.failure(RuntimeException("Firebase token retrieve failed. Task canceled")))
    }.addOnFailureListener {
        cont.resumeWith(Result.failure(it))
    }
}

suspend fun FirebaseInstanceId.getFirebaseToken(): Either<Exception, String> {
    return Either.of {
        this.instanceIdSuspend()
    }
}