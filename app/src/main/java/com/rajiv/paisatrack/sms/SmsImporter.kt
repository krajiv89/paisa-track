package com.rajiv.paisatrack.sms

import android.content.Context
import android.provider.Telephony
import com.rajiv.paisatrack.data.Store
import com.rajiv.paisatrack.data.Txn

/** Reads the existing SMS inbox once so there's data on first launch. */
object SmsImporter {
    fun importInbox(ctx: Context): Int {
        val out = ArrayList<Txn>()
        val cols = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE)
        ctx.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI, cols, null, null,
            Telephony.Sms.DATE + " DESC"
        )?.use { c ->
            val bi = c.getColumnIndex(Telephony.Sms.BODY)
            val di = c.getColumnIndex(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val body = c.getString(bi) ?: continue
                val date = if (di >= 0) c.getLong(di) else System.currentTimeMillis()
                SmsParser.parse(body, date)?.let { out.add(it) }
            }
        }
        return Store.addUnique(ctx, out)
    }
}
