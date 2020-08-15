package ru.relabs.kurjer.presentation.report

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class ListClickInterceptor : RecyclerView.OnItemTouchListener {
    var enabled = true

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean = !enabled

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}