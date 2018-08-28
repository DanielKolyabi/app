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
import ru.relabs.kurjer.ui.helpers.HintAnimationHelper
import ru.relabs.kurjer.ui.presenters.TaskListPresenter


class TaskListFragment : Fragment() {
    val presenter = TaskListPresenter(this)
    private lateinit var hintAnimationHelper: HintAnimationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintAnimationHelper = HintAnimationHelper(hint_container, hint_icon)
        hint_container.setOnClickListener {
            hintAnimationHelper.changeState()
        }
        start.setOnClickListener {
            presenter.onStartClicked()
        }
    }


    companion object {
        @JvmStatic
        fun newInstance() = TaskListFragment()
    }
}
