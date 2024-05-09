package com.novumlogic.todo.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList.OnListChangedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.google.android.material.textview.MaterialTextView
import com.novumlogic.todo.R
import com.novumlogic.todo.TodoViewModelFactory
import com.novumlogic.todo.data.local.Category
import com.novumlogic.todo.data.Result
import com.novumlogic.todo.data.local.Task
import com.novumlogic.todo.data.local.TaskWithCategoryAndPriorityNames
import com.novumlogic.todo.data.succeeded
import com.novumlogic.todo.databinding.DialogNewTaskBinding
import com.novumlogic.todo.ui.viewmodels.NewTaskViewModel
import com.novumlogic.todo.util.Util
import com.novumlogic.todo.util.toTaskWithCategoryAndPriorityNames
import com.novumlogic.todo.util.toTitleCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.measureTimeMillis

class NewTaskFragment : DialogFragment() {
    private lateinit var binding: DialogNewTaskBinding
    private var taskDateInMillis: Long? = null
    private val priorityMap = mutableMapOf<String,Int>()
    private lateinit var taskItemCallback: suspend (TaskWithCategoryAndPriorityNames?, String?) -> Unit
    private val viewModel: NewTaskViewModel by viewModels { TodoViewModelFactory }

    companion object {
        private const val ARGUMENT_TASK_DATE = "ARGUMENT_TASK_DATE"
        private const val ARGUMENT_FRAGMENT_DISMISS = "ARGUMENT_FRAGMENT_DISMISS"
    }

    private val TAG = javaClass.simpleName
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            taskDateInMillis = it.getLong(ARGUMENT_TASK_DATE)
            taskItemCallback =
                it.getSerializable(ARGUMENT_FRAGMENT_DISMISS) as suspend (TaskWithCategoryAndPriorityNames?, String?) -> Unit
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogNewTaskBinding.inflate(inflater, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            taskDateInMillis?.let {
                textTaskDateDisplay.text =
                    DateFormat.getDateInstance(DateFormat.DEFAULT, Locale.getDefault())
                        .format(Date(it))
            }
            toolbarNewTask.apply {
                setNavigationOnClickListener { dismiss() }
                setOnMenuItemClickListener {
                    return@setOnMenuItemClickListener when (it.itemId) {
                        R.id.action_save_task -> {

                            if (viewModel.inputFieldValidation()) {

                                val name = textInputTaskName.editText?.text.toString()
                                val emoji = textEmojiDisplay.text.toString()
                                val priority = priorityMap[textInputPriority.editText?.text.toString()] ?: throw Exception("error while mapping priority name to ordinal")
                                val categoryName =
                                    textInputCategory.editText?.text.toString().trim().toTitleCase()

                                lifecycleScope.launch(Dispatchers.IO) {
                                    val priorityId = viewModel.getPriorityId(priority).await() ?: throw Exception("error while fetching id for $priority")
                                    val formattedDate =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
                                            Date(taskDateInMillis!!)
                                        )
                                    val currentTime = System.currentTimeMillis()
                                    if (!viewModel.categoryList.contains(categoryName)) {
                                        val categoryInsertionResult =
                                            viewModel.insertCategory(categoryName, currentTime)
                                                .await()
                                        if (categoryInsertionResult.succeeded) {
                                            categoryInsertionResult as Result.Success
                                            val result = viewModel.saveTask(
                                                name,
                                                categoryInsertionResult.data.id,
                                                priorityId,
                                                emoji,
                                                formattedDate,
                                                currentTime
                                            ).await()
                                            savingTask(result,categoryInsertionResult.data.name,priority)
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                categoryInsertionResult.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        val cId = viewModel.getCategoryId(categoryName)
                                            ?: throw Exception("id not found for category name = $categoryName")
                                        val result = viewModel.saveTask(
                                            name,
                                            cId,
                                            priorityId,
                                            emoji,
                                            formattedDate,
                                            currentTime
                                        ).await()
                                        savingTask(result,categoryName,priority)
                                    }

                                }
                            }
                            true
                        }

                        else -> {
                            false
                        }
                    }
                }
            }


            textInputTaskName.editText?.run {
                setOnFocusChangeListener { view, b -> if (b) emojiPicker.visibility = View.GONE }
                setOnEditorActionListener(TextView.OnEditorActionListener { p0, p1, p2 ->
                    if (p1 == EditorInfo.IME_ACTION_NEXT) {
                        p0.clearFocus()
                        textInputCategory.requestFocus()
                        true
                    } else false
                })
            }

            textInputCategory.editText?.run {
                setOnFocusChangeListener { view, b -> if (b) emojiPicker.visibility = View.GONE }
                setOnEditorActionListener { textView, i, keyEvent ->
                    when (i) {
                        EditorInfo.IME_ACTION_DONE -> {
                            textView.clearFocus()
                            Util.closeKeyboard(textView)
                            true
                        }

                        else -> false
                    }
                }
            }


            val categoryArrayAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                viewModel.categoryList
            )
            (textInputCategory.editText as? AutoCompleteTextView)?.setAdapter(categoryArrayAdapter)

            val priorityArrayAdapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_list_item_1,
                viewModel.priorityList
            )
            (textInputPriority.editText as? AutoCompleteTextView)?.let {
                it.setAdapter(priorityArrayAdapter)
            }


            viewModel.categoryList.addOnListChangedCallback(object :
                OnListChangedCallback<ObservableArrayList<Category>>() {
                override fun onChanged(sender: ObservableArrayList<Category>?) {
                    Log.d(TAG, "categories list changed : $sender")
                }

                override fun onItemRangeChanged(
                    sender: ObservableArrayList<Category>?,
                    positionStart: Int,
                    itemCount: Int
                ) {
                }

                override fun onItemRangeInserted(
                    sender: ObservableArrayList<Category>?,
                    positionStart: Int,
                    itemCount: Int
                ) {
                    categoryArrayAdapter.notifyDataSetChanged()
                    Log.d(TAG, "categories items insertion : ${sender?.joinToString()}")
                }

                override fun onItemRangeMoved(
                    sender: ObservableArrayList<Category>?,
                    fromPosition: Int,
                    toPosition: Int,
                    itemCount: Int
                ) {
                }

                override fun onItemRangeRemoved(
                    sender: ObservableArrayList<Category>?,
                    positionStart: Int,
                    itemCount: Int
                ) {
                    categoryArrayAdapter.notifyDataSetChanged()
                    Log.d(TAG, "categories items removal: ${sender?.joinToString()}")
                }

            })

            viewModel.categoryListLiveData.observe(viewLifecycleOwner) {
                Toast.makeText(
                    requireContext(),
                    "${it.count()} categories fetched",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.apply {
                    categoryList.clear()
                    categoryList.addAll(it.map { c -> c.name }.sorted())
                }
            }

            viewModel.priorityListLiveData.observe(viewLifecycleOwner) {
                viewModel.apply {
                    priorityList.clear()
                    val list = it.map {p->
                        when (p.priority) {
                            0 -> {
                                priorityMap["Low"] = 0
                                "Low"
                            }
                            1 -> {
                                priorityMap["Medium"] = 1
                                "Medium"
                            }
                            else -> {
                                priorityMap["High"] = 2
                                "High"
                            }
                        }
                    }

                    priorityList.addAll(list)
                }
                Log.d(TAG, "onViewCreated: in priority list $it, ${priorityArrayAdapter.count}")
//                (textInputPriority.editText as AutoCompleteTextView).setText(priorityArrayAdapter.getItem(0).toString(),false)

            }


            viewModel.networkConnected.observe(viewLifecycleOwner) {
                if (it) {
                    viewModel.forceUpdateCategories()
                }
            }
            emojiPicker.setOnEmojiPickedListener {
                textEmojiDisplay.text = it.emoji
            }

            textEmojiDisplay.setOnClickListener {
                Util.closeKeyboard(it)
                textInputTaskName.editText?.clearFocus()
                textInputCategory.editText?.clearFocus()
                lifecycleScope.launch {
                    delay(250)
                    emojiPicker.visibility = View.VISIBLE
                }
            }
        }

    }

    private suspend fun savingTask(result: Result<Task>,categoryName: String, priority: Int) {
        withContext(Dispatchers.Main) {
            if (result.succeeded) {
                result as Result.Success
                val time = measureTimeMillis {
                    taskItemCallback.invoke(result.data.toTaskWithCategoryAndPriorityNames(categoryName,priority), null)
                }
                delay(time)
                dismiss()
            } else {
                result as Result.Failure
                Log.d(TAG, "savingTask: $result")
                taskItemCallback.invoke(null, result.exception.toString())
            }
        }
    }
}


private enum class Priority {
    LOW,
    MEDIUM,
    HIGH
}
