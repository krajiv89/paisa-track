package com.rajiv.paisatrack.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.rajiv.paisatrack.data.Store
import com.rajiv.paisatrack.widget.SpendWidget

/** Fires the moment a new SMS arrives. Parses it and stores if it's a transaction. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        // Multi-part SMS come as several PDUs; join their bodies.
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        val ts = msgs.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val txn = SmsParser.parse(body, ts) ?: return
        val added = Store.addUnique(context, listOf(txn))
        if (added > 0) SpendWidget.refresh(context)
    }
}
