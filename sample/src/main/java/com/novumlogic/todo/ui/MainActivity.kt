package com.novumlogic.todo.ui

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList
import androidx.databinding.ObservableList.OnListChangedCallback
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.novumlogic.todo.R
import com.novumlogic.todo.TodoViewModelFactory
import com.novumlogic.todo.data.local.TaskWithCategoryAndPriorityNames
import com.novumlogic.todo.databinding.ActivityMainBinding
import com.novumlogic.todo.ui.adapters.CompleteTaskAdapter
import com.novumlogic.todo.ui.adapters.IncompleteTaskAdapter
import com.novumlogic.todo.ui.viewmodels.MainViewModel
import com.novumlogic.todo.util.NpaGridLayoutManager
import com.novumlogic.todo.util.SwipeToDeleteCallback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {
    private val TAG = javaClass.simpleName
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels{ TodoViewModelFactory }
    private val completeTaskAdapter: CompleteTaskAdapter by lazy {
        val updateTaskListener:(TaskWithCategoryAndPriorityNames)->Unit ={viewModel.updateTask(it)}
        val deleteTaskListener:(TaskWithCategoryAndPriorityNames)->Unit ={viewModel.deleteTask(it,0)}
        CompleteTaskAdapter(updateTaskListener,deleteTaskListener)
    }
    private val incompleteTaskAdapter: IncompleteTaskAdapter by lazy {
        val updateTaskListener:(TaskWithCategoryAndPriorityNames)->Unit ={viewModel.updateTask(it)}
        val deleteTaskListener:(TaskWithCategoryAndPriorityNames)->Unit ={viewModel.deleteTask(it,1)}
        IncompleteTaskAdapter (updateTaskListener,deleteTaskListener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this
        binding.todayDate = Date(System.currentTimeMillis())

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.label_select_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        savedInstanceState?.let {
            if(it.containsKey(ARGUMENT_SAVE_LIST)){
                val bool = it.getBoolean(ARGUMENT_SAVE_LIST,false)
                if(bool){
                    incompleteTaskAdapter.submitList(viewModel.incompleteTaskList)
                    completeTaskAdapter.submitList(viewModel.completeTaskList)
                }
            }
        }

        viewModel.incompleteTaskList.addOnListChangedCallback(object: OnListChangedCallback<ObservableArrayList<TaskWithCategoryAndPriorityNames>>(){
            override fun onChanged(sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?) {
                Log.d(TAG, "onChanged: is changed")
            }

            override fun onItemRangeChanged(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeInserted(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
                incompleteTaskAdapter.submitList(sender)
                incompleteTaskAdapter.notifyItemRangeInserted(positionStart, itemCount)
                Log.d(TAG, "incomplete list -> onItemRangeInserted: $positionStart, $itemCount ")
            }

            override fun onItemRangeMoved(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                fromPosition: Int,
                toPosition: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeRemoved(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
                incompleteTaskAdapter.notifyItemRangeRemoved(positionStart,itemCount)
                Log.d(TAG, "incomplete list -> onItemRangeRemoved: $positionStart $itemCount ")
            }

        })

        viewModel.completeTaskList.addOnListChangedCallback(object: OnListChangedCallback<ObservableArrayList<TaskWithCategoryAndPriorityNames>>(){
            override fun onChanged(sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?) {
            }

            override fun onItemRangeChanged(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeInserted(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
                completeTaskAdapter.submitList(sender)
                completeTaskAdapter.notifyItemRangeInserted(positionStart,itemCount)
                Log.d(TAG, "complete list -> onItemRangeInserted: $positionStart $itemCount")
            }

            override fun onItemRangeMoved(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                fromPosition: Int,
                toPosition: Int,
                itemCount: Int
            ) {
            }

            override fun onItemRangeRemoved(
                sender: ObservableArrayList<TaskWithCategoryAndPriorityNames>?,
                positionStart: Int,
                itemCount: Int
            ) {
                completeTaskAdapter.notifyItemRangeRemoved(positionStart,itemCount)
                Log.d(TAG, "complete list -> onItemRangeRemoved: $positionStart $itemCount ")
            }

        })

        viewModel.taskList.observe(this){
            viewModel.apply {
                loadingStatus.postValue(true)
                completeTaskList.clear()
                incompleteTaskList.clear()
                val (newCompleteTaskList,newIncompleteTaskList) = it.partition { task -> task.isComplete }

                completeTaskList.addAll(newCompleteTaskList)
                incompleteTaskList.addAll(newIncompleteTaskList)

                totalTaskCount.set(it.count())
                incompleteTaskCount.set(newIncompleteTaskList.count())
                loadingStatus.postValue(false)
            }


        }
        
        binding.apply {
            textTaskDate.apply {
                setOnClickListener { if(!picker.isAdded) picker.show(supportFragmentManager, "TaskDatePicker") }
            }

            picker.addOnPositiveButtonClickListener {
                picker.let {
                    textTaskDate.text = it.headerText
                    viewModel.currentDate.value = it.let { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
                        Date(it.selection!!)
                    ) }

                    if(it.selection!! < MaterialDatePicker.todayInUtcMilliseconds())
                        binding.buttonAddTask.hide()
                    else
                        binding.buttonAddTask.show()

                }
                Log.d(TAG, "onCreate: ${viewModel.currentDate}")
            }

            listCompleteTask.apply {
                adapter = completeTaskAdapter.apply { setHasStableIds(true) }
                layoutManager = NpaGridLayoutManager(this@MainActivity, 1)
            }

            listIncompleteTask.apply {
                adapter = incompleteTaskAdapter.apply { setHasStableIds(true) }
                layoutManager = NpaGridLayoutManager(this@MainActivity, 1)
            }

            val inCompleteItemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(incompleteTaskAdapter))
            inCompleteItemTouchHelper.attachToRecyclerView(listIncompleteTask)

            val completeItemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(completeTaskAdapter))
            completeItemTouchHelper.attachToRecyclerView(listCompleteTask)

            buttonAddTask.setOnClickListener {
                val taskItemCallback: suspend (TaskWithCategoryAndPriorityNames?, String?) -> Unit = { task: TaskWithCategoryAndPriorityNames?, str:String? ->
                    task?.let {
                        withContext(Dispatchers.Main){
                            Toast.makeText(baseContext,getString(R.string.toast_task_saved), Toast.LENGTH_SHORT).show()
                            viewModel.incompleteTaskList.add(it)
                        }
                        viewModel.taskAddedToIncompleteList(true)
                    } ?: withContext(Dispatchers.Main){Toast.makeText(baseContext,str,Toast.LENGTH_SHORT).show()}

                }

                val dialog = NewTaskFragment().also {
                    it.arguments = Bundle().apply {
                        putLong(ARGUMENT_TASK_DATE, picker.selection!!)
                        putSerializable(ARGUMENT_FRAGMENT_DISMISS, taskItemCallback as Serializable)
                    }
                }
                dialog.show(supportFragmentManager, "Task_Fragment")
            }


            viewModel.networkConnected.observe(this@MainActivity, Observer {
                Log.d(TAG, "onCreate: Network connection on ?: $it, trigger point ")
                var str = if(it){
                    viewModel.forceUpdate()
                    getString(R.string.device_online)
                } else {
                    getString(R.string.device_offline)
                }
                Toast.makeText(this@MainActivity, str, Toast.LENGTH_SHORT).show()
            })

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.let {
            it.putBoolean(Companion.ARGUMENT_SAVE_LIST,true)
        }
    }

    companion object {
        private const val ARGUMENT_SAVE_LIST = "ARGUMENT_SAVE_LIST"
        private const val ARGUMENT_TASK_DATE = "ARGUMENT_TASK_DATE"
        private const val ARGUMENT_FRAGMENT_DISMISS = "ARGUMENT_FRAGMENT_DISMISS"
    }
}

