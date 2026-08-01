package com.rajiv.paisatrack.logic

import com.rajiv.paisatrack.data.Txn

/**
 * Turns an English sentence into a cash entry.
 *   "I spent rs 70 in tea shop"  -> 70, Tea shop, spent
 *   "I gave rs 1000 to wife"     -> 1000, Wife, given
 */
object ManualParser {

    private val AMOUNT = Regex("""(?:rs\.?|inr|₹)?\s*([\d,]+(?:\.\d+)?)\s*(k)?""", RegexOption.IGNORE_CASE)
    private val GAVE = Regex("""\b(gave|given|give|sent|paid to|handed)\b""", RegexOption.IGNORE_CASE)
    private val SPENT = Regex("""\b(spent|paid|bought|spend|purchased)\b""", RegexOption.IGNORE_CASE)
    private val WHO = Regex("""\b(?:in|at|to|for|on)\s+(.+)$""", RegexOption.IGNORE_CASE)

    data class Preview(val amount: Double, val merchant: String, val given: Boolean)

    fun parse(text: String): Preview? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        val am = AMOUNT.find(raw) ?: return null
        var amount = am.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (am.groupValues[2].isNotEmpty()) amount *= 1000   // "1k" -> 1000
        if (amount <= 0) return null

        val given = GAVE.containsMatchIn(raw) && !SPENT.containsMatchIn(raw)

        var who = WHO.find(raw)?.groupValues?.get(1)?.trim().orEmpty()
        // strip a trailing amount phrase if the sentence was "gave wife 1000"
        if (who.isEmpty()) {
            val after = raw.substringAfter(am.value, "").trim()
            who = after.ifEmpty { raw.substringBefore(am.value).trim() }
        }
        who = who.replace(Regex("""\b(rs\.?|inr|₹|today|now)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\d+"""), "").trim().trim('.', ',')
        if (who.isEmpty()) who = if (given) "Cash given" else "Cash spend"
        who = who.replaceFirstChar { it.uppercase() }

        return Preview(amount, who, given)
    }

    fun toTxn(p: Preview): Txn {
        val now = System.currentTimeMillis()
        return Txn(
            id = "manual-" + now + "-" + (0..9999).random(),
            bank = "Cash",
            kind = if (p.given) "Cash · given" else "Cash · spend",
            sourceType = "CASH",
            source = "Cash",
            amount = p.amount,
            dir = "DEBIT",
            merchant = p.merchant,
            epochMillis = now,
            hasTime = true,
            bal = null,
            manual = true
        )
    }
}
