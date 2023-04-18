package ru.relabs.kurjer.utils

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
suspend fun FirebaseMessaging.instanceIdAsync() = suspendCoroutine<String> { cont ->
    token.addOnSuccessListener {
        cont.resume(it)
    }
    token.addOnCanceledListener {
        cont.resumeWithException(RuntimeException("Coroutine canceled"))
    }
    token.addOnFailureListener {
        cont.resumeWithException(it)
    }
}