package com.kirtigames.abcd

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.random.Random

/**
 * A question young children can't answer, shown before anything meant for adults
 * (settings, progress, links). Google Play's Families policy requires one.
 */
object ParentGate {

    fun show(activity: Activity, onPass: () -> Unit) {
        val a = Random.nextInt(3, 10)
        val b = Random.nextInt(3, 10)
        val answer = a * b
        val options = (setOf(answer) + setOf(answer + a, answer - b, answer + b + 1, answer - a - 1)
            .filter { it > 0 && it != answer }.shuffled().take(2)).shuffled()

        fun pick(choice: Int) {
            if (choice == answer) onPass()
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("For grown-ups")
            .setMessage("To continue, tap the answer to  $a × $b")
            .setPositiveButton(options[0].toString()) { _, _ -> pick(options[0]) }
            .setNeutralButton(options[1].toString()) { _, _ -> pick(options[1]) }
            .setNegativeButton(options[2].toString()) { _, _ -> pick(options[2]) }
            .show()
    }
}
