package com.novumlogic.todo.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.novumlogic.todo.R
import com.novumlogic.todo.data.local.Task
import com.novumlogic.todo.data.local.TaskWithCategoryAndPriorityNames
import com.novumlogic.todo.databinding.ItemTaskCompleteBinding
import com.novumlogic.todo.util.toTask

class CompleteTaskAdapter(private val updateTaskListener: (TaskWithCategoryAndPriorityNames)->Unit,private val deleteTaskListener: (TaskWithCategoryAndPriorityNames)->Unit) : ListAdapter<TaskWithCategoryAndPriorityNames, CompleteTaskAdapter.ViewHolder>(DiffCallBack()) {

    private val TAG = javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTaskCompleteBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = getItem(position)
        holder.binding.apply {
            textCompleteTaskName.text = task.name
        }
        holder.setCompleteStatus(task.isComplete)
    }

    fun deleteItem(position: Int): TaskWithCategoryAndPriorityNames{
        val taskItem = getItem(position)
        taskItem.lastUpdatedTimestamp = System.currentTimeMillis()
        taskItem.isDelete = true
        taskItem.offlineFieldOpType = 3

        deleteTaskListener.invoke(taskItem)
        notifyItemRemoved(position)
        return taskItem
    }

    fun addItem(deletedItem: TaskWithCategoryAndPriorityNames, position: Int){
        deletedItem.lastUpdatedTimestamp = System.currentTimeMillis()
        deletedItem.isDelete = false
        deletedItem.offlineFieldOpType = 2

        //updating in view-model
        deleteTaskListener.invoke(deletedItem)
        notifyItemInserted(position)
    }
    inner class ViewHolder(val binding: ItemTaskCompleteBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val taskItem = getItem(bindingAdapterPosition)
                taskItem.isComplete = !taskItem.isComplete
                taskItem.lastUpdatedTimestamp = System.currentTimeMillis()
                taskItem.offlineFieldOpType = 2
                Log.d(TAG, "completeAdapter position:$bindingAdapterPosition -> $taskItem ")

                notifyItemChanged(bindingAdapterPosition)
                updateTaskListener.invoke(taskItem)
            }
        }

        fun setCompleteStatus(complete: Boolean){
            if(complete) binding.imageCompleteCheck.setImageResource(R.drawable.btn_background_task_checked)
            else binding.imageCompleteCheck.setImageResource(R.drawable.btn_background_task_unchecked)
        }
    }

    private class DiffCallBack: DiffUtil.ItemCallback<TaskWithCategoryAndPriorityNames>() {
        override fun areItemsTheSame(
            oldItem: TaskWithCategoryAndPriorityNames,
            newItem: TaskWithCategoryAndPriorityNames
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: TaskWithCategoryAndPriorityNames,
            newItem: TaskWithCategoryAndPriorityNames
        ): Boolean {
            return oldItem == newItem
        }

    }

}
