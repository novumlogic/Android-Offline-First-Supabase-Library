package com.novumlogic.todo.data.local

class CategoryLocalDataSource(val categoryDao: CategoryDao) {
    suspend fun insert(vararg category: Category) = categoryDao.insert(*category)
    fun getCategories() = categoryDao.getCategories()
    suspend fun getLocalChanges() = categoryDao.getLocalChanges()
    suspend fun setCrudByNames(nameList: List<String>, crud: Int) = categoryDao.setCrudByNames(nameList,crud)
    suspend fun setCrudById(id: Int, crud: Int) = categoryDao.setCrudById(id,crud)
    suspend fun update(category: Category) = categoryDao.update(category)
    suspend fun getCategoryByName(name: String) = categoryDao.getCategoryByName(name)
    suspend fun getCategoryIdByName(name: String) = categoryDao.getCategoryIdByName(name)
    suspend fun getLastCategoryId() = categoryDao.getLastCategoryId()
}

