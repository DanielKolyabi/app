package ru.relabs.kurjer.utils

import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.selects.select
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.DeliveryApp


/**
 * Created by ProOrange on 05.09.2018.
 */


fun application(): DeliveryApp {
    return DeliveryApp.appContext as DeliveryApp
}

fun Fragment.activity(): MainActivity? {
    return this.context as? MainActivity
}

fun Throwable.logError() {
    this.printStackTrace()

    FirebaseCrashlytics.getInstance().recordException(this)
    val stacktrace = CustomLog.getStacktraceAsString(this)
    CustomLog.writeToFile(stacktrace)
}

suspend fun <T> tryOrLogAsync(block: suspend () -> T) {
    try {
        block()
    } catch (e: Exception) {
        e.logError()
    }
}

fun <T> tryOrLog(block: () -> T) {
    try {
        block()
    } catch (e: Exception) {
        e.logError()
    }
}

suspend fun <E : Job> Iterable<E>.joinFirst(): E = select {
    for (job in this@joinFirst) {
        job.onJoin { job }
    }
}

suspend fun <E : Deferred<R>, R> Iterable<E>.awaitFirst(): R = joinFirst().getCompleted()