package com.novumlogic.todo.data.local

class PriorityLocalDataSource(private val priorityDao: PriorityDao) {
    fun getPriorities() = priorityDao.getAll()
    suspend fun getPriorityId(priority: Int) = priorityDao.getPriorityId(priority)
}