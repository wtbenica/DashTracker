package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import com.wtb.dashTracker.MainActivity.Companion.APP
import java.util.*

class TableRadioButton(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatRadioButton(context, attrs) {

    private lateinit var mGroup: String

    init {
        addToGroup(attrs)
    }

    private fun addToGroup(attrs: AttributeSet) {
        (0..attrs.attributeCount).forEach { i ->
            val attributeName = attrs.getAttributeName(i)
            if (attributeName.equals("group")) {
                val groupName: String = attrs.getAttributeValue(i)
                var group: TableRadioGroup? = groups[groupName]

                if (group != null) {
                    group.addView(this)
                } else {
                    group = TableRadioGroup()
                    group.addView(this)

                    groups[groupName] = group
                }

                mGroup = groupName
                setOnClickListener(group)
                return
            }
        }

        val group = TableRadioGroup()
        group.addView(this)

        val rn = Random()
        var groupName: String
        do {
            groupName = rn.nextInt().toString()
        } while (groups.containsKey(groupName))

        mGroup = groupName
        setOnClickListener(group)
    }

    fun getRadioGroup() = groups[mGroup]

    companion object {
        private const val TAG = APP + "TableRadioButton"

        private val groups = mutableMapOf<String, TableRadioGroup>()
    }
}

class TableRadioGroup(val name: String = Random().nextInt().toString()) : OnClickListener {
    private val buttons = mutableListOf<TableRadioButton>()
    var callback: TableRadioGroupCallback? = null

    override fun onClick(v: View?) {
        var selectedButton: TableRadioButton? = null
        buttons.forEach {
            (it == v).let { b ->
                it.isChecked = b
                if (b) selectedButton = it
            }
        }
        selectedButton?.let { callback?.onCheckChanged(it) }
    }

    fun addView(button: TableRadioButton) {
        Log.d(TAG, "Adding a button to $name")
        buttons.add(button)
    }

    fun setChecked(button: TableRadioButton) = onClick(button)

    interface TableRadioGroupCallback {
        fun onCheckChanged(button: TableRadioButton)
    }

    companion object {
        private const val TAG = APP + "TableRadioButton"
    }
}