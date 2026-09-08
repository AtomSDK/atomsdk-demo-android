/*
 * Copyright (c) 2018 ATOM SDK Demo.
 * All rights reserved.
 */

package com.atom.vpn.demo.common

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.atom.sdk.android.AtomManager
import com.atom.sdk.android.PauseVPNTimer
import com.atom.vpn.demo.AtomDemoAppCallback
import com.atom.vpn.demo.atom

/**
 * Members are annotated [JvmStatic] so the remaining Java sources can keep using
 * `import static com.atom.vpn.demo.common.Utilities.changeButtonState` and friends. Those
 * annotations can be dropped once the migration is complete.
 */
object Utilities {

    @JvmStatic
    fun setViewAndChildrenEnabled(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setViewAndChildrenEnabled(view.getChildAt(i), enabled)
            }
        }
    }

    @JvmStatic
    fun changeButtonState(button: Button?, text: String) {
        button?.postDelayed({
            try {
                button.text = text
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 1000)
    }

    @JvmStatic
    fun getPauseTimerList(activity: Activity, callback: AtomDemoAppCallback<PauseVPNTimer>) {
        val timers = listOf(
            PauseVPNTimer.MINUTES_5, PauseVPNTimer.MINUTES_10, PauseVPNTimer.MINUTES_15,
            PauseVPNTimer.MINUTES_30, PauseVPNTimer.MINUTES_60, PauseVPNTimer.MANUAL
        )
        val times = timers.map { it.toString() }.toTypedArray()
        UIHelper.showListDialogBox(activity, "Pause VPN for:", times) { _, which ->
            callback.invoke(timers[which])
        }
    }

    @JvmStatic
    fun changeButtonText(context: Context, button: Button?) {
        val atomManager = atom ?: return
        val status = atomManager.getCurrentVpnStatus(context)
        button?.text = when {
            status.equals(AtomManager.VPNStatus.CONNECTED, ignoreCase = true) -> "Disconnect"
            status.equals(AtomManager.VPNStatus.CONNECTING, ignoreCase = true) -> "Cancel"
            else -> "Connect"
        }
    }
}
