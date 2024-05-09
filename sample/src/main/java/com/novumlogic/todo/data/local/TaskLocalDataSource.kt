package com.novumlogic.todo.data.local

import androidx.lifecycle.LiveData

class TaskLocalDataSource(val taskDao: TaskDao) {

    suspend fun insert(vararg task: Task): Array<Long> = taskDao.insert(*task)
    fun getTasksByDate(date: String): LiveData<List<Task>> = taskDao.getTasksByDate(date)
    fun getTasksWithCategoryAndPriorityNames(date: String): LiveData<List<TaskWithCategoryAndPriorityNames>> = taskDao.getTasksWithCategoryAndPriorityNames(date)

    suspend fun getTaskById(id: Int) = taskDao.getTaskById(id)

    suspend fun updateTask(task: Task): Int = taskDao.updateTask(task)
    suspend fun getLastLocalId() = taskDao.getLastLocalId()
    suspend fun setCrudById(id: Int, crud: Int) = taskDao.setCrudById(id,crud)
    suspend fun setCrudById(idsList: List<Int>, crud: Int) = taskDao.setCrudById(idsList,crud)
    suspend fun getLocalChanges() = taskDao.getLocalChanges()


}