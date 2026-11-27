package com.suzy.ai.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.suzy.ai.service.CallAnnouncerService

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallAnnouncerService.onPhoneRinging(context.applicationContext, incomingNumber)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK,
            TelephonyManager.EXTRA_STATE_IDLE -> {
                CallAnnouncerService.onPhoneEndedOrAnswered(context.applicationContext)
            }
        }
    }
}
