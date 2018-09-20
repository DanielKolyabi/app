package ru.relabs.kurjer.ui.fragments


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_task_item_explanation.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.models.TaskItemModel


class TaskItemExplanationFragment : Fragment() {
    lateinit var item: TaskItemModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getParcelable("task_item")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_item_explanation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(view){
            val noteTextViews = listOf(note1_text, note2_text, note3_text)
            item.notes.forEachIndexed { i, note ->
                if(note.isNullOrBlank()){
                    noteTextViews[i].text = note
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(item: TaskItemModel) =
                TaskItemExplanationFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("task_item", item)
                    }
                }
    }
}
