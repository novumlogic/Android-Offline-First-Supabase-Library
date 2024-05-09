package com.novumlogic.todo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.supabaseofflinesupport.BaseSyncableEntity
import kotlinx.serialization.Serializable

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) override val id: Int,
    val name: String,
    override var lastUpdatedTimestamp: Long,
    override val offlineFieldOpType: Int
) : BaseSyncableEntity()

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        Category::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    ), ForeignKey(
        Priority::class,
        ["id"],
        ["priority_id"],
        ForeignKey.CASCADE,
        ForeignKey.CASCADE
    )]
)
data class Task(
    @PrimaryKey override val id: Int,
    val name: String,
    @ColumnInfo("category_id") val categoryId: Int,
    val emoji: String,
    val date: String,
    @ColumnInfo("is_complete") var isComplete: Boolean,
    @ColumnInfo("priority_id") val priorityId: Int,
    override var lastUpdatedTimestamp: Long,
    @ColumnInfo("is_delete") var isDelete: Boolean,
    override var offlineFieldOpType: Int
) : BaseSyncableEntity()

data class TaskWithCategoryAndPriorityNames(
    val id: Int,
    val name: String,
    val emoji: String,
    @ColumnInfo("category_id") val categoryId: Int,
    @ColumnInfo("priority_id") val priorityId: Int,
    @ColumnInfo("category_name") val categoryName: String,
    val priority: Int,
    val date: String,
    @ColumnInfo("is_complete") var isComplete: Boolean,
    var lastUpdatedTimestamp: Long,
    @ColumnInfo("is_delete") var isDelete: Boolean,
    var offlineFieldOpType: Int
)

enum class OfflineCrudType { NONE, INSERT, UPDATE, DELETE }

@Serializable
@Entity("priorities")
data class Priority(@PrimaryKey val id: Int, val priority: Int)