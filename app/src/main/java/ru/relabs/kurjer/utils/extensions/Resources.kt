package ru.relabs.kurjer.utils.extensions

import android.content.res.Resources
import android.os.Build

/**
 * Created by Daniil Kurchanov on 19.12.2019.
 */
fun Resources.getColorCompat(id: Int): Int {
    return if(Build.VERSION.SDK_INT > 23){
        getColor(id, null)
    }else{
        getColor(id)
    }
}