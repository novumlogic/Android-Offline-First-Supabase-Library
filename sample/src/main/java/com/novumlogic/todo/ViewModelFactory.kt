package com.novumlogic.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.novumlogic.todo.ui.viewmodels.MainViewModel
import com.novumlogic.todo.ui.viewmodels.NewTaskViewModel

val TodoViewModelFactory = object: ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = with(modelClass){
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as TodoApplication
        val taskRepo = application.taskRepository
        when{
            isAssignableFrom(MainViewModel::class.java) -> MainViewModel(taskRepo)
            isAssignableFrom(NewTaskViewModel::class.java) -> NewTaskViewModel(taskRepo)
            else -> throw IllegalArgumentException("Unknown Viewmodel class ${modelClass.name}")
        }
    } as T
}