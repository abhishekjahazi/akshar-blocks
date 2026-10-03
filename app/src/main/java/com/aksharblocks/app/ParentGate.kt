package com.aksharblocks.app

import android.app.Activity
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference
import kotlin.random.Random

/**
 * A question young children can't answer, shown before anything meant for adults
 * (settings, progress). Google Play's Families policy requires one. The answer is typed,
 * not picked from buttons, so random tapping can't get through.
 */
object ParentGate {

    /** The gate on screen now; another tap on the lock doesn't stack a second question on top. */
    private var open: WeakReference<AlertDialog>? = null

    fun show(activity: Activity, onPass: () -> Unit) {
        if (open?.get()?.isShowing == true) return

        val a = Random.nextInt(3, 10)
        val b = Random.nextInt(3, 10)
        val answer = a * b

        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(3))
            hint = "Answer"
            textSize = 22f
            imeOptions = EditorInfo.IME_ACTION_DONE
            isSingleLine = true
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
        // The keyboard's Done key works like OK. A hardware Enter key arrives as IME_NULL,
        // once for key down and once for key up; submit on the up.
        input.setOnEditorActionListener { _, action, event ->
            val enter = action == EditorInfo.IME_NULL && event?.keyCode == KeyEvent.KEYCODE_ENTER
            if (action != EditorInfo.IME_ACTION_DONE && !enter) return@setOnEditorActionListener false
            if (event == null || event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                if (input.text.toString().trim().toIntOrNull() == answer) onPass()
            }
            true
        }
        dialog.setOnDismissListener { open = null }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        open = WeakReference(dialog)
        dialog.show()
        input.requestFocus()
    }
}
