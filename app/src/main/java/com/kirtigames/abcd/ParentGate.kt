package com.kirtigames.abcd

import android.app.Activity
import android.text.InputFilter
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.random.Random

/**
 * A question young children can't answer, shown before anything meant for adults
 * (settings, progress). Google Play's Families policy requires one. The answer is typed,
 * not picked from buttons, so random tapping can't get through.
 */
object ParentGate {

    fun show(activity: Activity, onPass: () -> Unit) {
        val a = Random.nextInt(3, 10)
        val b = Random.nextInt(3, 10)
        val answer = a * b

        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
            hint = "Answer"
            textSize = 22f
        }
        val box = FrameLayout(activity).apply {
            val side = (24 * activity.resources.displayMetrics.density).toInt()
            setPadding(side, 0, side, 0)
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("For grown-ups")
            .setMessage("To continue, type the answer to  $a × $b")
            .setView(box)
            .setPositiveButton("OK") { _, _ ->
                if (input.text.toString().trim().toIntOrNull() == answer) onPass()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
        input.requestFocus()
    }
}
