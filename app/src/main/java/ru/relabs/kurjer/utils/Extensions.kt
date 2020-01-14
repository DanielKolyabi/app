package ru.relabs.kurjer.utils

import android.support.v4.app.Fragment
import com.crashlytics.android.Crashlytics
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.selects.select
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication


/**
 * Created by ProOrange on 05.09.2018.
 */


fun application(): MyApplication {
    return MyApplication.instance
}

fun Fragment.activity(): MainActivity? {
    return this.context as? MainActivity
}

fun Throwable.logError() {
    this.printStackTrace()

    Crashlytics.logException(this)
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