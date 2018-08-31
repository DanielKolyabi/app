package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_address_list.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressListModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.AddressListAddressDelegate
import ru.relabs.kurjer.ui.delegates.AddressListSortingDelegate
import ru.relabs.kurjer.ui.delegates.AddressListTaskItemDelegate
import ru.relabs.kurjer.ui.helpers.HintAnimationHelper
import ru.relabs.kurjer.ui.presenters.AddressListPresenter

class AddressListFragment : Fragment() {
    val presenter = AddressListPresenter(this)
    private lateinit var hintAnimationHelper: HintAnimationHelper
    lateinit var tasks: List<TaskModel>
    val adapter = DelegateAdapter<AddressListModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            tasks = it.getParcelableArrayList("tasks")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_address_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hintAnimationHelper = HintAnimationHelper(hint_container, hint_icon)

        hint_container.setOnClickListener {
            hintAnimationHelper.changeState()
        }

        adapter.apply {
            addDelegate(AddressListAddressDelegate(tasks.size == 1))
            addDelegate(AddressListTaskItemDelegate(
                    { addressId -> presenter.onItemClicked(addressId) }
            ))
            addDelegate(AddressListSortingDelegate(
                    { presenter.changeSortingMethod(it) }
            ))
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        if(adapter.data.size == 0) {
            presenter.tasks.addAll(tasks)
            presenter.applySorting()
        }
    }


    companion object {
        fun newInstance(tasks: List<TaskModel>) =
                AddressListFragment().apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList("tasks", ArrayList(tasks))
                    }
                }
    }
}

