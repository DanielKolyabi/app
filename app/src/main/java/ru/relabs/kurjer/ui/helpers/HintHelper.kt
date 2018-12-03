package ru.relabs.kurjer.ui.helpers

import android.content.SharedPreferences
import android.support.constraint.ConstraintLayout
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import kotlinx.android.synthetic.main.include_hint_container.view.*

/**
 * Created by ProOrange on 27.08.2018.
 */
class HintHelper(val hintContainer: View, val text: String, private var expanded: Boolean = false, val preferences: SharedPreferences) {

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
        if(spFontSize < 12 || spFontSize > 26) return
        preferences.edit().putFloat("hint_font_size", spFontSize).apply()
        hintContainer.hint_text.textSize = spFontSize
//        if(expanded) {
//            hintContainer.layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
//            hintContainer.requestLayout()
//        }
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
        hintContainer.measure(View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val lp = (hintContainer.hint_icon.layoutParams as ConstraintLayout.LayoutParams)
        val collapsedHeight = hintContainer.hint_icon.height + lp.topMargin + lp.bottomMargin
        val expandedHeight = Math.min(hintContainer.measuredHeight, 200*hintContainer.resources.displayMetrics.density.toInt())

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
        hintContainer.measure(View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val lp = hintContainer.hint_icon.layoutParams as ConstraintLayout.LayoutParams
        val collapsedHeight = hintContainer.hint_icon.height + lp.topMargin + lp.bottomMargin
        val expandedHeight = Math.min(hintContainer.measuredHeight, 200*hintContainer.resources.displayMetrics.density.toInt())

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hintContainer.layoutParams.height = if (interpolatedTime == 1f)
                    collapsedHeight
                else
                    (expandedHeight - (expandedHeight - collapsedHeight) * interpolatedTime).toInt()
                hintContainer.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }
}