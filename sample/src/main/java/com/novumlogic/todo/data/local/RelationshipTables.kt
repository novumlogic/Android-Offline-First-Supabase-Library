package com.novumlogic.todo.data.local

import androidx.room.Embedded
import androidx.room.Relation

//Defines the one-to-many relationship between category table and task table
//i.e. One category belongs to zero or more instances of tasks
data class CategoryWithTasks(
    @Embedded val category: Category,
    @Relation(parentColumn = "name", entityColumn = "category")
    val tasks: List<Task>
)

//TODO(Check this for error, as column name priority is used in both tables i.e. Priority and Task, if required use prefix attribute of @Embedded annotation )
data class PriorityWithTasks(
    @Embedded val priority: Priority,
    @Relation(parentColumn = "priority", entityColumn = "priority")
    val tasks: List<Task>
)

