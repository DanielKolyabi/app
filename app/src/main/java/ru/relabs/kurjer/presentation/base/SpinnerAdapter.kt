package ru.relabs.kurjer.presentation.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter

/**
 * Created by Daniil Kurchanov on 10.09.2019.
 */
class SpinnerAdapter<T>(
    ctx: Context,
    val layout: Int,
    val binder: (holder: View, item: T?) -> Unit
) :
    ArrayAdapter<T>(ctx, layout) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val holder = convertView ?: LayoutInflater.from(context).inflate(layout, null)
        binder(holder, item)
        return holder
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val holder = convertView ?: LayoutInflater.from(context).inflate(layout, null)
        binder(holder, item)
        return holder
    }
}