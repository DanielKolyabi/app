package ru.relabs.kurjer.uiOld.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_task_details.*
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.storage.AuthTokenStorage
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.models.TaskModel.Companion.TASK_STATE_MASK
import ru.relabs.kurjer.data.database.AppDatabase
import ru.relabs.kurjer.uiOld.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsHeaderDelegate
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsInfoDelegate
import ru.relabs.kurjer.uiOld.delegates.TaskDetailsItemDelegate
import ru.relabs.kurjer.uiOld.models.DetailsListModel
import ru.relabs.kurjer.uiOld.presenters.TaskDetailsPresenter

class TaskDetailsFragment : Fragment() {

    val database: AppDatabase by inject()

    val presenter = TaskDetailsPresenter(this, database, get())
    val adapter = DelegateAdapter<DetailsListModel>()
    lateinit var task: TaskModel
    var posInList = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = it.getParcelable("task")!!
            posInList = it.getInt("pos_in_list")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_details, container, false)
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
                    task.items.sortedWith(compareBy<TaskItemModel> { it.subarea }
                            .thenBy { it.bypass }
                            .thenBy { it.address.city }
                            .thenBy { it.address.street }
                            .thenBy { it.address.house }
                            .thenBy { it.address.houseName }
                            .thenBy { it.state }
                    ).groupBy {
                        it.address.id
                    }.toList().sortedBy {
                        !it.second.any { it.state != TaskItemModel.CLOSED }
                    }.toMap().flatMap {
                        it.value
                    }.map { DetailsListModel.TaskItem(it) }
            )

            adapter.notifyDataSetChanged()
        }
        val state = task.state and TASK_STATE_MASK
        examine_button.isEnabled = state == 0
    }

    companion object {

        @JvmStatic
        fun newInstance(task: TaskModel, posInList: Int = 0) =
                TaskDetailsFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("task", task)
                        putInt("pos_in_list", posInList)
                    }
                }
    }
}
