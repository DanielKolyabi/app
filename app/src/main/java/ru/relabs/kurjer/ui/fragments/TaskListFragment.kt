package ru.relabs.kurjer.ui.fragments


import android.app.ActionBar
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_task_list.*
import ru.relabs.kurjer.R


class TaskListFragment : Fragment() {
    var hintCollapsed = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hint_container.setOnClickListener {
            setHintExpanded(!hintCollapsed)
            hintCollapsed = !hintCollapsed
        }
    }

    private fun setHintExpanded(expanded: Boolean) {
        val anim = if (expanded) getCollapseHintAnimation() else getExpandHintAnimation()
        hint_container.startAnimation(anim.apply {
            duration = 1000
        });
    }

    private fun getExpandHintAnimation(): Animation {
        hint_container.measure(View.MeasureSpec.makeMeasureSpec(hint_container.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val lp = (hint_icon.layoutParams as LinearLayout.LayoutParams)
        val collapsedHeight = hint_icon.height+lp.topMargin+lp.bottomMargin
        val expandedHeight = hint_container.measuredHeight

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hint_container.layoutParams.height = if (interpolatedTime == 1f)
                    expandedHeight
                else
                    (collapsedHeight + (expandedHeight - collapsedHeight) * interpolatedTime).toInt()
                hint_container.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }

    private fun getCollapseHintAnimation(): Animation {
        hint_container.measure(View.MeasureSpec.makeMeasureSpec(hint_container.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        val lp = hint_icon.layoutParams as LinearLayout.LayoutParams
        val collapsedHeight = hint_icon.height+lp.topMargin+lp.bottomMargin
        val expandedHeight = hint_container.measuredHeight

        return object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                hint_container.layoutParams.height = if (interpolatedTime == 1f)
                    collapsedHeight
                else
                    (expandedHeight - (expandedHeight - collapsedHeight) * interpolatedTime).toInt()
                hint_container.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TaskListFragment()
    }
}
