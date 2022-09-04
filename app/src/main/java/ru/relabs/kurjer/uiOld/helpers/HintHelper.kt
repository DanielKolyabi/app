package ru.relabs.kurjer.uiOld.helpers

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.include_hint_container.view.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.utils.CustomLog

/**
 * Created by ProOrange on 27.08.2018.
 */
@SuppressLint("ClickableViewAccessibility")
class HintHelper(
    val hintContainer: View,
    private var expanded: Boolean = true,
    val preferences: SharedPreferences,
) {
    var text: CharSequence = ""
        set(value) {
            field = value
            hintContainer.hint_text?.text = value
            setHintExpanded(expanded)
        }
    var maxHeight: Int = 200 * hintContainer.resources.displayMetrics.density.toInt()
        set(value) {
            field = value
            setHintExpanded(expanded)
        }

    constructor(
        hintContainer: View,
        text: String,
        expanded: Boolean = false,
        activity: Activity
    ) : this(hintContainer, expanded, activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)) {
        this.text = text
    }

    private var expandedHeight = 0

    init {
        hintContainer.isFocusable = true
        hintContainer.isClickable = true
        hintContainer.setOnClickListener {
            changeState()
        }
        hintContainer.hint_text.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && !hintContainer.hint_text.hasSelection())
                changeState()
            false
        }
        hintContainer.font_plus.setOnClickListener {
            setFontBigger()
        }
        hintContainer.font_minus.setOnClickListener {
            setFontSmaller()
        }

        changeFont(preferences.getFloat("hint_font_size", 12f))
    }

    private fun changeFont(spFontSize: Float) {
        if (spFontSize < 12 || spFontSize > 26) return
        preferences.edit().putFloat("hint_font_size", spFontSize).apply()
        hintContainer.hint_text.textSize = spFontSize
    }

    private fun setFontSmaller() {
        val curFont = hintContainer.hint_text.textSize / hintContainer.resources.displayMetrics.scaledDensity
        changeFont(curFont - 2)
    }

    private fun setFontBigger() {
        val curFont = hintContainer.hint_text.textSize / hintContainer.resources.displayMetrics.scaledDensity
        changeFont(curFont + 2)
    }

    fun changeState() {
        expanded = !expanded
        setHintExpanded(expanded)
    }

    private fun setHintExpanded(expanded: Boolean) {
        val anim = if (!expanded) getCollapseHintAnimation() else getExpandHintAnimation()
        hintContainer.startAnimation(anim.apply {
            duration = 250
        })
    }

    fun TextView.calculateHeight(text: CharSequence = getText()): Int {
        val alignment = when (gravity) {
            Gravity.CENTER -> Layout.Alignment.ALIGN_CENTER
            Gravity.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, TextPaint(paint), width)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setAlignment(alignment)
                .setIncludePad(true).build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text, TextPaint(paint), width, alignment,
                lineSpacingMultiplier, lineSpacingExtra, true
            )
        }.height
    }

    private fun getExpandHintAnimation(): Animation {
        hintContainer.measure(
            View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val collapsedHeight = hintContainer.height
        val minHeight = 250 * hintContainer.resources.displayMetrics.density.toInt()
        expandedHeight = maxOf(
            minHeight,
            minOf(
                maxHeight,
                hintContainer.hint_text.calculateHeight(text)
            )
        )

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hintContainer.layoutParams.height = if (interpolatedTime == 1f)
                    expandedHeight
                else
                    (collapsedHeight + (expandedHeight - collapsedHeight) * interpolatedTime).toInt()
                hintContainer.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }

    private fun getCollapseHintAnimation(): Animation {
        hintContainer.measure(
            View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val lp = hintContainer.hint_icon.layoutParams as ConstraintLayout.LayoutParams
        val collapsedHeight = hintContainer.hint_icon.height + lp.topMargin + lp.bottomMargin

        val currentHeight = hintContainer.height

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hintContainer.layoutParams.height = if (interpolatedTime == 1f)
                    collapsedHeight
                else
                    (currentHeight - (currentHeight - collapsedHeight) * interpolatedTime).toInt()
                hintContainer.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }
}