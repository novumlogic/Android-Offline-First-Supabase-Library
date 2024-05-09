package com.novumlogic.todo.util

import androidx.databinding.ObservableArrayList
import androidx.lifecycle.MutableLiveData
import com.novumlogic.todo.data.CategoryDto
import com.novumlogic.todo.data.TaskDto
import com.novumlogic.todo.data.local.Category
import com.novumlogic.todo.data.local.Task
import com.novumlogic.todo.data.local.TaskWithCategoryAndPriorityNames

fun <T> MutableLiveData<T>.forceRefreshData() {
    this.value = this.value
}

fun Task.toDto(): TaskDto = TaskDto(
    this.id,
    this.name,
    this.categoryId,
    this.emoji,
    this.isComplete,
    this.date,
    this.priorityId,
    this.lastUpdatedTimestamp,
    this.isDelete
)

fun TaskDto.toEntity(crud: Int): Task = Task(
    this.id,
    this.name,
    this.category_id,
    this.emoji,
    this.date,
    this.isComplete,
    this.priority_id,
    this.lastUpdatedTimestamp,
    this.isDelete,
    crud
)

fun Category.toDto(): CategoryDto = CategoryDto(
    this.name,
    this.lastUpdatedTimestamp,
    this.id
)

fun CategoryDto.toEntity(crud: Int): Category =
    Category(
        this.id,
        this.name,
        this.lastUpdatedTimestamp,
        crud
    )

fun String.toTitleCase(): String =
    this.split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercaseChar() } }

fun ObservableArrayList<TaskWithCategoryAndPriorityNames>?.toTaskList(): List<Task>? {
    return this?.map {
        Task(
            it.id,
            it.name,
            it.categoryId,
            it.emoji,
            it.date,
            it.isComplete,
            it.priorityId,
            it.lastUpdatedTimestamp,
            it.isDelete,
            it.offlineFieldOpType
        )
    }
}

fun Task.toTaskWithCategoryAndPriorityNames(
    categoryName: String,
    priority: Int
): TaskWithCategoryAndPriorityNames {
    return TaskWithCategoryAndPriorityNames(
        this.id,
        this.name,
        this.emoji,
        this.categoryId,
        this.priorityId,
        categoryName,
        priority,
        this.date,
        this.isComplete,
        this.lastUpdatedTimestamp,
        this.isDelete,
        this.offlineFieldOpType
    )
}


fun TaskWithCategoryAndPriorityNames.toTask(): Task {
    return Task(
        this.id,
        this.name,
        this.categoryId,
        this.emoji,
        this.date,
        this.isComplete,
        this.priorityId,
        this.lastUpdatedTimestamp,
        this.isDelete,
        this.offlineFieldOpType
    )
}

