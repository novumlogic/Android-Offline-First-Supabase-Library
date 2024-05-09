package com.novumlogic.todo.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.novumlogic.todo.R
import com.novumlogic.todo.ui.adapters.CompleteTaskAdapter
import com.novumlogic.todo.ui.adapters.IncompleteTaskAdapter

class SwipeToDeleteCallback(private val adapter: RecyclerView.Adapter<*>): ItemTouchHelper.Callback() {
    private val TAG = javaClass.simpleName
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
       return makeMovementFlags(0,ItemTouchHelper.LEFT)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        Log.d(TAG, "onSwiped: position $position and abs posn -> ${viewHolder.absoluteAdapterPosition}")
        if(adapter is IncompleteTaskAdapter){
            val deletedItem = adapter.deleteItem(position)

            Snackbar.make(viewHolder.itemView,"Item Deleted",Snackbar.LENGTH_LONG).also { it.setAction("Undo"){
                adapter.addItem(deletedItem,position)
            } }.show()
        }else{
            adapter as CompleteTaskAdapter
            val deletedItem = adapter.deleteItem(position)

            Snackbar.make(viewHolder.itemView,"Item Deleted",Snackbar.LENGTH_LONG).also { it.setAction("Undo"){
                adapter.addItem(deletedItem,position)
            } }.show()
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        Log.d(TAG, "onChildDraw: dx= $dX, dy= $dY, actionState- $actionState, isCurrentlyActive-$isCurrentlyActive, canvas-$c")
        if(dX<0){
            val itemView = viewHolder.itemView
            val p = Paint()
            p.color = Color.RED

            val background = RectF(itemView.right + dX,itemView.top.toFloat(),itemView.right.toFloat(),itemView.bottom.toFloat())
            c.drawRect(background,p)

            val icon = ContextCompat.getDrawable(viewHolder.itemView.context, R.drawable.ic_delete)
            icon?.let {
                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                it.setBounds(
                    itemView.right - iconMargin - icon.intrinsicWidth,
                    iconTop,
                    itemView.right - iconMargin,
                    iconBottom
                )
                it.draw(c)
            }
        }
    }


}