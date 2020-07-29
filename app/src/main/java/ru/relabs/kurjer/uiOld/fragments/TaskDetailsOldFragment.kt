package ru.relabs.kurjer.uiOld.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_task_details_old.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.domain.models.Task
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.TaskItemState
import ru.relabs.kurjer.domain.models.TaskState
import ru.relabs.kurjer.presentation.taskDetails.IExaminedConsumer
import ru.relabs.kurjer.uiOld.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsHeaderDelegate
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsInfoDelegate
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsItemDelegate
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.uiOld.presenters.TaskDetailsPresenter

class TaskDetailsOldFragment : Fragment() {

    val database: AppDatabase by inject()

    val presenter = TaskDetailsPresenter(this, database, get())
    val adapter = DelegateAdapter<DetailsListModel>()
    lateinit var task: Task
    var posInList = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = it.getParcelable("task")!!
            posInList = it.getInt("pos_in_list")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_details_old, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        examine_button.setOnClickListener {
            presenter.onExaminedClicked(task)
        }

        show_map_button.setOnClickListener {
            presenter.onMapClicked(task)
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        adapter.addDelegate(TaskDetailsHeaderDelegate())
        adapter.addDelegate(TaskDetailsInfoDelegate())
        adapter.addDelegate(TaskDetailsItemDelegate(
            { presenter.onInfoClicked(it) }
        ))


        if (adapter.data.isEmpty()) {
            adapter.data.add(DetailsListModel.Task(task))
            adapter.data.add(DetailsListModel.DetailsTableHeader)
            adapter.data.addAll(
                task.items.sortedWith(compareBy<TaskItem> { it.subarea }
                    .thenBy { it.bypass }
                    .thenBy { it.address.city }
                    .thenBy { it.address.street }
                    .thenBy { it.address.house }
                    .thenBy { it.address.houseName }
                    .thenBy { it.state }
                ).groupBy {
                    it.address.id
                }.toList().sortedBy {
                    !it.second.any { it.state != TaskItemState.CLOSED }
                }.toMap().flatMap {
                    it.value
                }.map { DetailsListModel.TaskItem(it) }
            )

            adapter.notifyDataSetChanged()
        }
        val state = task.state.state
        examine_button.isEnabled = state == TaskState.CREATED
    }

    companion object {

        @JvmStatic
        fun <T> newInstance(task: Task, parent: T) where T : Fragment, T : IExaminedConsumer =
            TaskDetailsOldFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("task", task)
                    putInt("pos_in_list", posInList)
                }
            }
    }
}
