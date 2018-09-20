package ru.relabs.kurjer.ui.fragments


import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_address_list.*
import kotlinx.android.synthetic.main.include_hint_container.*
import ru.relabs.kurjer.BuildConfig
import ru.relabs.kurjer.R
import ru.relabs.kurjer.activity
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.delegates.AddressListAddressDelegate
import ru.relabs.kurjer.ui.delegates.AddressListLoaderDelegate
import ru.relabs.kurjer.ui.delegates.AddressListSortingDelegate
import ru.relabs.kurjer.ui.delegates.AddressListTaskItemDelegate
import ru.relabs.kurjer.ui.helpers.HintHelper
import ru.relabs.kurjer.ui.models.AddressListModel
import ru.relabs.kurjer.ui.presenters.AddressListPresenter

class AddressListFragment : Fragment() {
    val presenter = AddressListPresenter(this)
    private lateinit var hintHelper: HintHelper
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
        hintHelper = HintHelper(hint_container, resources.getString(R.string.address_list_hint_text), false, activity!!.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE))

        adapter.apply {
            addDelegate(AddressListAddressDelegate(
                    {
                        this@AddressListFragment.activity()?.showYandexMap(it)
                    },
                    tasks.size == 1
            ))
            addDelegate(AddressListLoaderDelegate())
            addDelegate(AddressListTaskItemDelegate(
                    { addressId, taskId -> presenter.onItemClicked(addressId, taskId) },
                    { task -> presenter.onItemMapClicked(task) }
            ))
            addDelegate(AddressListSortingDelegate(
                    { presenter.changeSortingMethod(it) }
            ))
        }

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = adapter

        if (adapter.data.size == 0) {
            adapter.data.add(AddressListModel.Loader)
            adapter.notifyDataSetChanged()

            presenter.tasks.addAll(tasks)
            presenter.applySorting()
        } else {
            adapter.data.clear()
            adapter.data.add(AddressListModel.Loader)
            adapter.notifyDataSetChanged()

            presenter.updateStates()
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

