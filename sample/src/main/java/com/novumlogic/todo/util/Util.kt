package com.novumlogic.todo.util

import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi

object Util {
    @JvmStatic fun closeKeyboard(view: View) {
        val imm = view.context.applicationContext.getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(view.windowToken,0)
    }
}