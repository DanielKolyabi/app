package ru.relabs.kurjer.presentation.dialogs

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_reject.view.*
import kotlinx.android.synthetic.main.holder_spinner.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.presentation.base.SpinnerAdapter
import ru.relabs.kurjer.utils.extensions.visible

class RejectFirmDialog(val reasons: List<String>, val onRejected: (reason: String) -> Unit) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_AppCompat_Light_Dialog_Alert)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_reject, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true

        val adapter = SpinnerAdapter<RejectReason>(requireContext(), R.layout.holder_spinner) { v, data ->
            v.tv_content.text = data?.name ?: ""
        }
        adapter.addAll(
            reasons.map { RejectReason(it, false) } +
                    listOf(
                        RejectReason("Другое", true)
                    )
        )
        view.sp_reason.adapter = adapter

        view.sp_reason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                this@RejectFirmDialog.view?.et_reason?.visible = adapter.getItem(position)?.shouldExpandDescription == true
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        view.btn_reject.setOnClickListener {
            val item = adapter.getItem(view.sp_reason.selectedItemPosition)
            if (item?.shouldExpandDescription == true) {
                onRejected(view.et_reason.text.toString())
            } else {
                onRejected(item?.name ?: "")
            }
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }

    internal data class RejectReason(val name: String, val shouldExpandDescription: Boolean)
}