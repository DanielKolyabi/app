package ru.relabs.kurjer

import android.support.v4.app.Fragment


/**
 * Created by ProOrange on 05.09.2018.
 */

fun Fragment.application(): MyApplication?{
    return this.activity?.application as MyApplication
}

fun Fragment.activity(): MainActivity{
    return this.context as MainActivity
}