package ru.relabs.kurjer.utils

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Daniil Kurchanov on 13.01.2020.
 */
suspend fun FirebaseInstanceId.instanceIdAsync() = suspendCoroutine<InstanceIdResult> { cont ->
    instanceId.addOnSuccessListener {
        cont.resume(it)
    }
    instanceId.addOnCanceledListener {
        cont.resumeWithException(RuntimeException("Coroutine canceled"))
    }
    instanceId.addOnFailureListener {
        cont.resumeWithException(it)
    }
}