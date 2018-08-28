package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_address_list.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.AddressElement
import ru.relabs.kurjer.ui.adapters.AddressDelegate
import ru.relabs.kurjer.ui.adapters.TaskAdapter
import ru.relabs.kurjer.ui.delegateAdapter.DelegateAdapter
import ru.relabs.kurjer.ui.helpers.HintAnimationHelper
import ru.relabs.kurjer.ui.presenters.AddressListPresenter

class AddressListFragment : Fragment() {
    val presenter = AddressListPresenter(this)
    private lateinit var hintAnimationHelper: HintAnimationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val d = DelegateAdapter<AddressElement>()

        d.apply {
            addAdapter(AddressDelegate())
            addAdapter(TaskAdapter())
        }
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = d

        d.data.add(AddressElement.AddressModel("test"))
        d.data.add(AddressElement.TaskModel("test2"))
        d.data.add(AddressElement.TaskModel("test3"))
        d.data.add(AddressElement.TaskModel("test4"))
        d.data.add(AddressElement.AddressModel("test5"))
        d.data.add(AddressElement.AddressModel("test6"))

        d.notifyDataSetChanged()
    }

    companion object {
        fun newInstance() =
                AddressListFragment()
    }
}
