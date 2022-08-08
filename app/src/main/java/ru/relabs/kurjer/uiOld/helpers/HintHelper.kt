package ru.relabs.kurjer.uiOld.helpers

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.include_hint_container.view.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.utils.CustomLog

/**
 * Created by ProOrange on 27.08.2018.
 */
class HintHelper(
    val hintContainer: View,
    val text: String,
    private var expanded: Boolean = false,
    val preferences: SharedPreferences,
    var maxHeight: Int = 200 * hintContainer.resources.displayMetrics.density.toInt()
) {

    constructor(
        hintContainer: View,
        text: String,
        expanded: Boolean = false,
        activity: Activity
    ) : this(hintContainer, text, expanded, activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE))

    var expandedHeight = 0

    init {
        hintContainer.hint_text.text = text
        hintContainer.setOnClickListener {
            changeState()
        }
        hintContainer.hint_text.setOnClickListener {
            changeState()
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
        setHintExpanded(expanded)
        expanded = !expanded
    }


    private fun setHintExpanded(expanded: Boolean) {
        val anim = if (expanded) getCollapseHintAnimation() else getExpandHintAnimation()
        hintContainer.startAnimation(anim.apply {
            duration = 250
        })
    }

    private fun getExpandHintAnimation(): Animation {
        hintContainer.measure(
            View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val lp = (hintContainer.hint_icon.layoutParams as ConstraintLayout.LayoutParams)
        val collapsedHeight = hintContainer.hint_icon.height + lp.topMargin + lp.bottomMargin
        expandedHeight = maxHeight

        CustomLog.writeToFile("" +
                "Expand hint from $collapsedHeight to $expandedHeight, default = ${200 * hintContainer.resources.displayMetrics.density.toInt()}\n"+
                "  text height = ${hintContainer.hint_text.height}, container height = ${hintContainer.height}"
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

        val actualExpandedHeight = expandedHeight.takeIf { it != 0 } ?: maxHeight

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hintContainer.layoutParams.height = if (interpolatedTime == 1f)
                    collapsedHeight
                else
                    (actualExpandedHeight - (actualExpandedHeight - collapsedHeight) * interpolatedTime).toInt()
                hintContainer.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }
}