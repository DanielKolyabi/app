package ru.relabs.kurjer.ui.holders

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.support.v4.graphics.ColorUtils
import android.view.View
import kotlinx.android.synthetic.main.item_addr_list_task.view.*
import ru.relabs.kurjer.models.TaskItemModel
import ru.relabs.kurjer.models.TaskModel
import ru.relabs.kurjer.ui.delegateAdapter.BaseViewHolder
import ru.relabs.kurjer.ui.models.AddressListModel
import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import ru.relabs.kurjer.R


/**
 * Created by ProOrange on 11.08.2018.
 */
class AddressListTaskItemHolder(
        itemView: View,
        val onItemClicked: (item: AddressListModel.TaskItem) -> Unit,
        private val onItemMapClicked: (task: TaskModel) -> Unit) : BaseViewHolder<AddressListModel>(itemView) {

    private fun getValueAnimator(from: Int, to: Int, view: View?, duration: Int = 250): ValueAnimator{
        return ValueAnimator.ofObject(ArgbEvaluator(), from, to).apply{
            setDuration(duration.toLong())
            addUpdateListener { animator -> view?.setBackgroundColor(animator.animatedValue as Int) }
        }
    }

    private fun startFlashValueAnimator(view: View?, onAnimationEnd: (() -> Unit)? = null){
        val targetColor = itemView.resources.getColor(R.color.colorAccent)
        val colorFrom = ColorUtils.setAlphaComponent(targetColor, 0)
        val colorTo = targetColor

        val colorAnimationTo = getValueAnimator(colorFrom, colorTo, view, 500)
        val colorAnimationFrom = getValueAnimator(colorTo, colorFrom, view, 500)
        onAnimationEnd?.let{
            colorAnimationFrom.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    it()
                }
            })
        }
        colorAnimationTo.addListener(object: AnimatorListenerAdapter(){
            override fun onAnimationEnd(animation: Animator?) {
                colorAnimationFrom.start()
            }
        })

        colorAnimationTo.start()
    }

    fun flashSelectedColor(){

        startFlashValueAnimator(itemView){
            startFlashValueAnimator(itemView)
        }
    }

    override fun onBindViewHolder(item: AddressListModel) {
        if (item !is AddressListModel.TaskItem) return
        itemView.task_button.text = "${item.parentTask.name} №${item.parentTask.edition}, ${item.taskItem.copies}экз."


        if (item.taskItem.state == TaskItemModel.CLOSED) {
            //itemView.task_button.isEnabled = false
            itemView.map_icon.alpha = 0.4f
            itemView.map_icon.isClickable = false
            itemView.task_button.setTextColor(Color.parseColor("#66000000"))
        } else {
            //itemView.task_button.isEnabled = true
            itemView.map_icon.alpha = 1f
            itemView.map_icon.isClickable = true
            itemView.task_button.setTextColor(Color.parseColor("#ff000000"))


            if (item.taskItem.needPhoto){
                itemView.task_button.setTextColor(itemView.resources.getColor(R.color.colorFuchsia))
            }else{
                itemView.task_button.setTextColor(Color.parseColor("#ff000000"))
            }
        }



        itemView.task_button.setOnClickListener {
            onItemClicked(item)
        }
        itemView.map_icon.setOnClickListener {
            onItemMapClicked(item.parentTask)
        }
    }
}