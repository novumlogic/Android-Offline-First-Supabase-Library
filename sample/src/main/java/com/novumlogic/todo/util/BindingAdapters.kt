package com.novumlogic.todo.util

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

@BindingAdapter("app:errorText")
fun errorText(view: TextInputLayout, text: String?){
    if(text.isNullOrBlank()){
        view.isErrorEnabled = false
    }else{
        view.error = text
        view.isErrorEnabled = true
    }
}