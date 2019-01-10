package ru.relabs.kurjer


/**
 * Created by ProOrange on 27.09.2018.
 */


class MyExceptionHandler : Thread.UncaughtExceptionHandler {

    private val defaultUEH: Thread.UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        e.logError()

        defaultUEH.uncaughtException(t, e)
    }

}