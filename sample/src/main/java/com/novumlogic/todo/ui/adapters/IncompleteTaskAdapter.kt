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
import com.novumlogic.todo.databinding.ItemTaskIncompleteBinding
import com.novumlogic.todo.util.toTask

class IncompleteTaskAdapter(private val updateTaskListener: (TaskWithCategoryAndPriorityNames)-> Unit,private val deleteTaskListener: (TaskWithCategoryAndPriorityNames)->Unit) : ListAdapter<TaskWithCategoryAndPriorityNames, IncompleteTaskAdapter.ViewHolder>(DiffCallBack()) {

    private val TAG = javaClass.simpleName
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemTaskIncompleteBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = getItem(position)
        val taskPriorityString = when(task.priority){
            0-> "Low"
            1-> "Medium"
            else-> "High"
        }
        holder.binding.apply {
            textIncompleteTaskName.text = task.name
            textTaskCategory.text = "${task.emoji}  ${task.categoryName}"
            textTaskPriority.text = holder.itemView.context.getString(R.string.task_priority,taskPriorityString)
        }
        holder.changeIsComplete(task.isComplete)
    }

    fun deleteItem(position: Int): TaskWithCategoryAndPriorityNames {
        val taskItem = getItem(position)
        taskItem.lastUpdatedTimestamp = System.currentTimeMillis()
        taskItem.isDelete = true
        taskItem.offlineFieldOpType = 3

        //updating in view-model
        deleteTaskListener.invoke(taskItem)
//        currentList.removeAt(position)
        notifyItemRemoved(position)
        return taskItem
    }

    fun addItem(deleteItem: TaskWithCategoryAndPriorityNames, position: Int) {
        deleteItem.lastUpdatedTimestamp = System.currentTimeMillis()
        deleteItem.isDelete = false
        deleteItem.offlineFieldOpType = 2

        //updating in view-model
        deleteTaskListener.invoke(deleteItem)
//        currentList.add(position,deleteItem)
        notifyItemInserted(position)
    }

    inner class ViewHolder(val binding: ItemTaskIncompleteBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener{
                val taskItem = getItem(bindingAdapterPosition)
                taskItem.isComplete = !taskItem.isComplete
                taskItem.lastUpdatedTimestamp = System.currentTimeMillis()
                taskItem.offlineFieldOpType = 2

                Log.d(TAG, "incompleteadapter : posn -> $bindingAdapterPosition , task -> $taskItem")
                notifyItemChanged(bindingAdapterPosition)
                updateTaskListener.invoke(taskItem)
            }
        }

        fun changeIsComplete(b: Boolean) {
            if(b)
                binding.imageIncompleteCheck.setImageResource(R.drawable.btn_background_task_checked)
            else
                binding.imageIncompleteCheck.setImageResource(R.drawable.btn_background_task_unchecked)
        }

    }


    private class DiffCallBack : DiffUtil.ItemCallback<TaskWithCategoryAndPriorityNames>(){
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
