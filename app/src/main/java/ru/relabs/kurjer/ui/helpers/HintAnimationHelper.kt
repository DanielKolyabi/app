package ru.relabs.kurjer.ui.helpers

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_task_list.*
import kotlin.math.exp

/**
 * Created by ProOrange on 27.08.2018.
 */
class HintAnimationHelper(val hintContainer: View, private val hintIcon: View, private var expanded: Boolean = false) {

    fun changeState(){
        setHintExpanded(expanded)
        expanded = !expanded
    }


    private fun setHintExpanded(expanded: Boolean) {
        val anim = if (expanded) getCollapseHintAnimation() else getExpandHintAnimation()
        hintContainer.startAnimation(anim.apply {
            duration = 250
        });
    }

    private fun getExpandHintAnimation(): Animation {
        hintContainer.measure(View.MeasureSpec.makeMeasureSpec(hintContainer.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val lp = (hintIcon.layoutParams as LinearLayout.LayoutParams)
        val collapsedHeight = hintIcon.height+lp.topMargin+lp.bottomMargin
        val expandedHeight = hintContainer.measuredHeight

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

        val lp = hintIcon.layoutParams as LinearLayout.LayoutParams
        val collapsedHeight = hintIcon.height+lp.topMargin+lp.bottomMargin
        val expandedHeight = hintContainer.measuredHeight

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