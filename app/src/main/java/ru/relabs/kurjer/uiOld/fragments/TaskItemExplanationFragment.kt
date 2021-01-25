package ru.relabs.kurjer.uiOld.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_task_item_explanation.view.*
import ru.relabs.kurjer.R
import ru.relabs.kurjer.domain.models.TaskItem
import ru.relabs.kurjer.domain.models.notes
import ru.relabs.kurjer.models.TaskItemModel


class TaskItemExplanationFragment : Fragment() {
    lateinit var item: TaskItem
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getParcelable("task_item")!!
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
                if(!note.isNullOrBlank()){
                    noteTextViews[2-i].text = Html.fromHtml(note)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(item: TaskItem) =
                TaskItemExplanationFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable("task_item", item)
                    }
                }
    }
}
