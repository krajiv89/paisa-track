package com.rajiv.paisatrack.data

import org.json.JSONObject

/** One money movement. Comes from an SMS or a manual entry. */
data class Txn(
    val id: String,
    val bank: String,
    val kind: String,
    val sourceType: String,   // CARD, ACCOUNT, CASH
    val source: String,       // e.g. XX8752, *5067, Cash
    val amount: Double,
    val dir: String,          // DEBIT or CREDIT
    val merchant: String,
    val epochMillis: Long,
    val hasTime: Boolean,
    val bal: Double?,
    val manual: Boolean = false
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("bank", bank); put("kind", kind)
        put("sourceType", sourceType); put("source", source)
        put("amount", amount); put("dir", dir); put("merchant", merchant)
        put("epochMillis", epochMillis); put("hasTime", hasTime)
        put("bal", bal ?: JSONObject.NULL); put("manual", manual)
    }

    companion object {
        fun fromJson(o: JSONObject) = Txn(
            id = o.getString("id"),
            bank = o.getString("bank"),
            kind = o.getString("kind"),
            sourceType = o.getString("sourceType"),
            source = o.getString("source"),
            amount = o.getDouble("amount"),
            dir = o.getString("dir"),
            merchant = o.getString("merchant"),
            epochMillis = o.getLong("epochMillis"),
            hasTime = o.getBoolean("hasTime"),
            bal = if (o.isNull("bal")) null else o.getDouble("bal"),
            manual = o.optBoolean("manual", false)
        )
    }
}
