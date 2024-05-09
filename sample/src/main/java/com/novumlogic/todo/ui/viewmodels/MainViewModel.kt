package com.novumlogic.todo.ui.viewmodels

import android.util.Log
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.novumlogic.todo.data.TaskRepository
import com.novumlogic.todo.data.local.TaskWithCategoryAndPriorityNames
import com.novumlogic.todo.util.toTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(private val taskRepository: TaskRepository): ViewModel() {
    private val TAG = javaClass.simpleName

//    private val taskList = MutableLiveData<List<Task>>()
//    val incompleteTaskList = taskList.switchMap { getTaskList(it,false) }
//    val completeTaskList = taskList.switchMap { getTaskList(it,true) }
    val completeTaskList: ObservableArrayList<TaskWithCategoryAndPriorityNames> = ObservableArrayList()
    val incompleteTaskList = ObservableArrayList<TaskWithCategoryAndPriorityNames>()

    val incompleteTaskCount = ObservableInt(0)
    val totalTaskCount = ObservableInt(0)

//    var currentDate: String = DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault()).format(
//        Date(System.currentTimeMillis())
//    )
//    var currentDate: String = SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH).format(
//        Date(System.currentTimeMillis())
//    )

    val currentDate = MutableLiveData(SimpleDateFormat("yyyy-MM-dd",Locale.ENGLISH).format(
        Date(System.currentTimeMillis())
    ))

    val taskList = currentDate.switchMap { taskRepository.observeTasks(it).distinctUntilChanged() }

    val networkConnected = taskRepository.networkConnected.distinctUntilChanged()

    val loadingStatus = MutableLiveData<Boolean>()
//    val loadingStatus: LiveData<Boolean> = _loadingStatus



    fun updateTask(task: TaskWithCategoryAndPriorityNames) {
        viewModelScope.launch(Dispatchers.IO) {
            val update = taskRepository.updateTask(task.toTask())
            Log.d(TAG, "updateTask: in viewmodel : $update $task")
            }
        if(task.isComplete){
            incompleteTaskList.remove(task)
            completeTaskList.add(task)
            incompleteTaskCount.set(incompleteTaskCount.get()-1)
        }else{
            completeTaskList.remove(task)
            incompleteTaskList.add(task)
            incompleteTaskCount.set(incompleteTaskCount.get()+1)
        }

    }

    //from denotes from which adapter the item is being delete i.e 0 for complete, 1 for incomplete
    fun deleteTask(task: TaskWithCategoryAndPriorityNames, from: Int) {
        viewModelScope.launch(Dispatchers.IO){
            val deleteCount = taskRepository.updateTask(task.toTask())
            Log.d(TAG, "deleteTask: in viewmodel count: $deleteCount")
        }

        //being deleted from incomplete list
        if(from == 1){
            if(task.isDelete){
                incompleteTaskList.remove(task)
                taskAddedToIncompleteList(false)
            }else{
                taskAddedToIncompleteList(true)
            }
        }else{
            //being deleted from complete list
            if(task.isDelete){
                completeTaskList.remove(task)
                totalTaskCount.set(totalTaskCount.get()-1)
            }else{
                totalTaskCount.set(totalTaskCount.get()+1)
            }
        }

    }

    fun taskAddedToIncompleteList(status: Boolean) {
        if(status){
            totalTaskCount.set(totalTaskCount.get()+1)
            incompleteTaskCount.set(incompleteTaskCount.get()+1)
        }else{
            totalTaskCount.set(totalTaskCount.get()-1)
            incompleteTaskCount.set(incompleteTaskCount.get()-1)
        }
    }

    fun forceUpdate() {
        loadingStatus.postValue(true)
        viewModelScope.launch (Dispatchers.IO){
            async(Dispatchers.IO) {
                taskRepository.forceUpdateCategories()
                taskRepository.forceUpdateTasks()
            }
            loadingStatus.postValue(false)
        }
    }



}