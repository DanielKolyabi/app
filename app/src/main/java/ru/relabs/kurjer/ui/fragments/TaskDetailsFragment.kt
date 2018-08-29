package ru.relabs.kurjer.ui.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_task_details.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.DataModels
import ru.relabs.kurjer.models.DetailsTableHeader
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.TaskDetailsHeaderDelegate
import ru.relabs.kurjer.ui.delegates.TaskDetailsInfoDelegate
import ru.relabs.kurjer.ui.delegates.TaskDetailsItemDelegate
import ru.relabs.kurjer.ui.presenters.TaskDetailsPresenter

class TaskDetailsFragment : Fragment() {

    val presenter = TaskDetailsPresenter(this)
    val adapter = DelegateAdapter<DataModels>()
    lateinit var task: TaskModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = it.getParcelable("task")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        adapter.addDelegate(TaskDetailsHeaderDelegate())
        adapter.addDelegate(TaskDetailsInfoDelegate())
        adapter.addDelegate(TaskDetailsItemDelegate())

        adapter.data.add(task)
        adapter.data.add(DetailsTableHeader)
        adapter.data.addAll(task.items)

        adapter.notifyDataSetChanged()
    }

    companion object {

        @JvmStatic
        fun newInstance(task: TaskModel) =
                TaskDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("task", task)
                    }
                }
    }
}
