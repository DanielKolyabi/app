package ru.relabs.kurjer.presentation.customViews.drawables

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import ru.relabs.kurjer.R
import ru.relabs.kurjer.utils.debug

/**
 * Created by Daniil Kurchanov on 27.03.2020.
 */

class NavDrawerBackgroundDrawable(resources: Resources) : Drawable() {

    private val backgroundDrawable = resources.getDrawable(R.drawable.bg_nav_drawer, null)
    private var topPadding = resources.getDimensionPixelSize(R.dimen.status_bar_height)

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
    }

    override fun getIntrinsicWidth(): Int {
        return -1
    }

    override fun getIntrinsicHeight(): Int {
        return -1
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int = PixelFormat.TRANSPARENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
    }

    override fun draw(canvas: Canvas) {
        backgroundDrawable.setBounds(0, topPadding, bounds.right, bounds.bottom)
        backgroundDrawable.draw(canvas)
    }

}