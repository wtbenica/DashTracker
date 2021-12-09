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
                var group = groups[groupName]

                if (group != null) {
                    group.addView(this)
                } else {
                    group = TableRadioGroup()
                    group.addView(this)
                    mGroup = groupName

                    groups[groupName] = group
                }
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

    override fun onClick(v: View?) {
        buttons.forEach {
            it.isChecked = it == v
        }
    }

    fun addView(button: TableRadioButton) {
        Log.d(TAG, "Adding a button to $name")
        buttons.add(button)
    }

    companion object {
        private const val TAG = APP + "TableRadioButton"
    }
}