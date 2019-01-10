package ru.relabs.kurjer

import android.support.v4.app.Fragment


/**
 * Created by ProOrange on 05.09.2018.
 */


fun application(): MyApplication{
    return MyApplication.instance
}
fun Fragment.activity(): MainActivity?{
    return this.context as? MainActivity
}

fun Throwable.logError(){
    this.printStackTrace()

    val stacktrace = CustomLog.getStacktraceAsString(this)
    CustomLog.writeToFile(stacktrace)
}