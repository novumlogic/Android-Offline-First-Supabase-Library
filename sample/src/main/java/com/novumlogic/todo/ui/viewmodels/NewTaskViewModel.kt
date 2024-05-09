package com.novumlogic.todo.ui.viewmodels

import androidx.databinding.ObservableArrayList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.novumlogic.todo.data.local.Category
import com.novumlogic.todo.data.Result
import com.novumlogic.todo.data.local.Task
import com.novumlogic.todo.data.TaskRepository
import com.novumlogic.todo.util.forceRefreshData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class NewTaskViewModel(private val taskRepository: TaskRepository) :
    ViewModel() {
    private val TAG = javaClass.simpleName
    val taskName = MutableLiveData<String>()
    val taskCategory = MutableLiveData<String>()
    val taskPriority = MutableLiveData<String>( )

    val taskNameError: LiveData<String> = taskName.map { if (it.isNullOrBlank()) "Field cannot be empty" else "" }
    val taskCategoryError = taskCategory.map { if (it.isNullOrBlank()) "Field cannot be empty" else "" }
    val taskPriorityError = taskPriority.map { if (it.isNullOrBlank()) "Choose one" else "" }

    val categoryList = ObservableArrayList<String>()
    val categoryListLiveData = taskRepository.observeCategories().distinctUntilChanged()

    val priorityList = ObservableArrayList<String>()
    val priorityListLiveData = taskRepository.getPriorities()

    val networkConnected = taskRepository.networkConnected.distinctUntilChanged()

//    init {
//        getCategories()
//    }
    suspend fun getCategoryId(name: String) = taskRepository.getCategoryId(name)


    fun inputFieldValidation(): Boolean {
        return when {
            taskName.value?.isBlank() == true || !taskName.isInitialized -> {
                taskName.forceRefreshData()
                false
            }

            taskCategory.value?.isBlank() == true || !taskCategory.isInitialized -> {
                taskCategory.forceRefreshData()
                false
            }
            taskPriority.value?.isBlank() == true || !taskPriority.isInitialized -> {
                taskPriority.forceRefreshData()
                false
            }
            else -> true
        }
    }

/*    private fun getCategories() {
        viewModelScope.launch(Dispatchers.IO) {

            val result = taskRepository.observeCategories()
            if (result is Result.Success) {
                _categoryFetching.postValue("Category fetched successfully")
                withContext(Dispatchers.Main) {
                    categoryList.clear()
                    categoryList.addAll(result.data.map { it.name }.sorted())
                }
            } else {
                _categoryFetching.postValue("Category fetching failed")
            }
        }
    }*/

    fun insertCategory(name: String,currentTime: Long): Deferred<Result<Category>> {
        return viewModelScope.async {
            val newId = taskRepository.getLatestCategoryId()
            taskRepository.insertCategory(Category(newId,name,currentTime,1))
        }
    }
    suspend fun saveTask(
        name: String,
        categoryId: Int,
        priorityId: Int,
        emoji: String,
        taskDate: String,
        timestamp: Long
    ): Deferred<Result<Task>> {
        val status = viewModelScope.async(Dispatchers.IO) {
            val newId = taskRepository.getLatestLocalId()

            taskRepository.insertTask(Task(newId,name,categoryId,emoji,taskDate,false,priorityId,timestamp,false,1))
        }
        return status
    }

    fun forceUpdateCategories() {
        viewModelScope.launch (Dispatchers.IO){
//            val status = viewModelScope.async {
                taskRepository.forceUpdateCategories()
//            }
//            if(status.await()){
//                Log.d(TAG, "inside forceupdatecategory calling getCategories")
//                getCategories()
//            }
        }
    }

    fun getPriorityId(priority: Int): Deferred<Int?> {
        return viewModelScope.async {
            taskRepository.getPriorityId(priority)
        }
    }

}

