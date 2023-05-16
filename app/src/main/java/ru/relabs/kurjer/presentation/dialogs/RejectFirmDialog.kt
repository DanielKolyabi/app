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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import ru.relabs.kurjer.R
import ru.relabs.kurjer.databinding.DialogRejectBinding
import ru.relabs.kurjer.databinding.HolderSpinnerBinding
import ru.relabs.kurjer.presentation.base.SpinnerAdapter
import ru.relabs.kurjer.utils.extensions.visible

class RejectFirmDialog(val reasons: List<String>, val onRejected: (reason: String) -> Unit) : DialogFragment() {

    private var onDismissListener: (() -> Unit)? = null

    fun setOnDismissListener(listener: (() -> Unit)?) {
        onDismissListener = listener
    }

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
        val binding = DialogRejectBinding.bind(view)

        val adapter = SpinnerAdapter<RejectReason>(requireContext(), R.layout.holder_spinner) { v, data ->
            val adapterBinding = HolderSpinnerBinding.bind(v)
            adapterBinding.tvContent.text = data?.name ?: ""
        }
        adapter.addAll(
            reasons.map { RejectReason(it, false) } +
                    listOf(
                        RejectReason("Другое", true)
                    )
        )
        binding.spReason.adapter = adapter

        binding.spReason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val fragView = this@RejectFirmDialog.view
                binding.etReason.visible = adapter.getItem(position)?.shouldExpandDescription == true
                binding.btnReject.isEnabled =
                    adapter.getItem(position)?.shouldExpandDescription == false || (( binding.etReason.text?.length ?: 0) >= 5)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.etReason.doAfterTextChanged {
            val isReasonVisible =
               binding.spReason.selectedItemPosition.let { adapter.getItem(it)?.shouldExpandDescription == false }
            binding.btnReject.isEnabled = isReasonVisible || (( binding.etReason.text?.length ?: 0) >= 5)
        }

        binding.btnReject.setOnClickListener {
            val item = adapter.getItem(binding.spReason.selectedItemPosition)
            if (item?.shouldExpandDescription == true) {
                onRejected(binding.etReason.text.toString())
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
        dialog.setOnDismissListener {
            onDismissListener?.invoke()
        }
        return dialog
    }

    internal data class RejectReason(val name: String, val shouldExpandDescription: Boolean)
}