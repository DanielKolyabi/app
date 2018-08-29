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
import ru.relabs.kurjer.ui.delegates.AddressDelegate
import ru.relabs.kurjer.ui.delegates.TaskDelegate
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

        list.layoutManager = LinearLayoutManager(context)
        list.adapter = DelegateAdapter<AddressElement>().apply {
            addDelegate(AddressDelegate())
            addDelegate(TaskDelegate())

            data.add(AddressElement.AddressModel("test"))
            data.add(AddressElement.TaskModel("test2"))
            data.add(AddressElement.TaskModel("test3"))
            data.add(AddressElement.TaskModel("test4"))
            data.add(AddressElement.AddressModel("test5"))
            data.add(AddressElement.AddressModel("test6"))

            notifyDataSetChanged()
        }
    }

    companion object {
        fun newInstance() =
                AddressListFragment()
    }
}
