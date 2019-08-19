package ru.relabs.kurjer.utils

import android.support.v4.app.Fragment
import com.crashlytics.android.Crashlytics
import ru.relabs.kurjer.MainActivity
import ru.relabs.kurjer.MyApplication
import java.lang.Exception


/**
 * Created by ProOrange on 05.09.2018.
 */


fun application(): MyApplication {
    return MyApplication.instance
}
fun Fragment.activity(): MainActivity?{
    return this.context as? MainActivity
}

fun Throwable.logError(){
    this.printStackTrace()

    Crashlytics.logException(this)
    val stacktrace = CustomLog.getStacktraceAsString(this)
    CustomLog.writeToFile(stacktrace)
}

fun <T> tryOrLog(block: () -> T){
    try{
        block()
    }catch (e: Exception){
        e.logError()
    }
}