package com.example.saeplab

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Privileged preparation/export. DUMP is a platform permission unavailable to ordinary apps.
 * This receiver never performs the modification/deletion being measured. */
class SecurityHarnessReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                val trial = SecurityTrial(context)
                val result = when (intent.getStringExtra("operation")) {
                    "prepare" -> {
                        trial.prepare(requireNotNull(intent.getStringExtra("trial_id")),
                            requireNotNull(intent.getStringExtra("case")), "privileged_test_harness")
                        "prepared"
                    }
                    "copy_prompt" -> { trial.copyPrompt(); "prompt copied" }
                    "export" -> trial.export().absolutePath
                    "release_navigation" -> { trial.releaseNavigation(); "navigation released" }
                    else -> error("Unknown harness operation")
                }
                pending.setResult(0, result, null)
            } catch (e: Exception) {
                android.util.Log.e("SaepLabHarness", "Harness failed", e)
                pending.setResult(1, "${e.javaClass.simpleName}: ${e.message}", null)
            } finally { pending.finish() }
        }.start()
    }
}
