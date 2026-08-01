package com.rajiv.paisatrack.sms

import com.rajiv.paisatrack.data.Txn
import java.security.MessageDigest
import java.util.Calendar

/**
 * Parses bank SMS for 5 banks. One pattern per format.
 * CREDIT vs DEBIT is detected so income is never counted as spend.
 * This is the exact logic proven in the prototype.
 */
object SmsParser {

    private data class Pat(
        val bank: String,
        val kind: String,
        val sourceType: String,
        val re: Regex,
        val build: (MatchResult) -> Fields
    )

    private data class Fields(
        val amount: String,
        val source: String,
        val date: String,
        val time: String?,
        val merchant: String,
        val dir: String,
        val bal: String? = null,
        val via: String? = null
    )

    private val PATTERNS = listOf(
        Pat("Axis", "Axis Card", "CARD",
            Regex("""Spent\s+INR\s+([\d,]+\.?\d*)[\s\S]*?Card no\.\s+(XX\d+)[\s\S]*?(\d{2}-\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})\s+IST\s*\n(.+?)\n[\s\S]*?Avl Limit:\s+INR\s+([\d,]+\.?\d*)""")
        ) { m -> Fields(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[5], "DEBIT", m.groupValues[6]) },

        Pat("Axis", "Axis UPI", "ACCOUNT",
            Regex("""INR\s+([\d,]+\.?\d*)\s+debited[\s\S]*?A/c no\.\s+(XX\d+)[\s\S]*?(\d{2}-\d{2}-\d{2}),\s+(\d{2}:\d{2}:\d{2})[\s\S]*?UPI/(P2M|P2A)/\d+/(.+)""")
        ) { m -> Fields(m.groupValues[1], m.groupValues[2], m.groupValues[3], m.groupValues[4], m.groupValues[6].trim(), "DEBIT", via = if (m.groupValues[5] == "P2M") "Merchant" else "Person") },

        Pat("HDFC", "HDFC Card", "CARD",
            Regex("""Spent\s+Rs\.([\d,]+\.?\d*)\s+On\s+HDFC Bank Card\s+(\d+)\s+At\s+(.+?)\s+On\s+(\d{4}-\d{2}-\d{2}):(\d{2}:\d{2}:\d{2})""")
        ) { m -> Fields(m.groupValues[1], m.groupValues[2], m.groupValues[4], m.groupValues[5], m.groupValues[3].trim(), "DEBIT") },

        Pat("Union Bank", "Union Bank", "ACCOUNT",
            Regex("""A/c\s+\*?(\d+)\s+(Debited|Credited)\s+for\s+Rs:([\d,]+\.?\d*)\s+on\s+(\d{2}-\d{2}-\d{4})\s+(\d{2}:\d{2}:\d{2})[\s\S]*?Avl Bal\s+Rs:([\d,]+\.?\d*)""")
        ) { m -> Fields(m.groupValues[3], "*" + m.groupValues[1], m.groupValues[4], m.groupValues[5], "Mobile Banking transfer", if (m.groupValues[2] == "Credited") "CREDIT" else "DEBIT", m.groupValues[6]) },

        Pat("Kotak", "Kotak UPI", "ACCOUNT",
            Regex("""Sent\s+Rs\.([\d,]+\.?\d*)\s+from\s+Kotak Bank AC\s+(X?\d+)\s+to\s+(.+?)\s+on\s+(\d{2}-\d{2}-\d{2})\.UPI Ref""")
        ) { m -> Fields(m.groupValues[1], m.groupValues[2], m.groupValues[4], null, m.groupValues[3].trim(), "DEBIT") },

        Pat("Indian Bank", "Indian Bank", "ACCOUNT",
            Regex("""Sent\s+Rs\.([\d,]+\.?\d*)\s+from\s+A/c\s+\*?(\d+)\s+on\s+(\d{2}-\d{2}-\d{2})\s+to\s+(.+?)\.RRN\s+\d+\.Avl Bal\s+Rs\.([\d,]+\.?\d*)""")
        ) { m -> Fields(m.groupValues[1], "*" + m.groupValues[2], m.groupValues[3], null, m.groupValues[4].trim(), "DEBIT", m.groupValues[5]) }
    )

    private fun num(s: String) = s.replace(",", "").toDouble()

    /** Body date -> epoch millis. If body has no time, use fallbackMillis (real SMS receipt time). */
    private fun epochOf(date: String, time: String?, fallbackMillis: Long): Long {
        val c = Calendar.getInstance()
        val y: Int; val mo: Int; val d: Int
        if (Regex("""^\d{4}-""").containsMatchIn(date)) {
            val p = date.split("-"); y = p[0].toInt(); mo = p[1].toInt(); d = p[2].toInt()
        } else {
            val p = date.split("-"); d = p[0].toInt(); mo = p[1].toInt()
            y = p[2].toInt().let { if (it < 100) it + 2000 else it }
        }
        if (time == null) {
            // keep the real receipt time-of-day, but pin the calendar date from the SMS body
            c.timeInMillis = fallbackMillis
            c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, mo - 1); c.set(Calendar.DAY_OF_MONTH, d)
            return c.timeInMillis
        }
        val t = time.split(":")
        c.set(y, mo - 1, d, t[0].toInt(), t[1].toInt(), t.getOrElse(2) { "0" }.toInt())
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun sha(s: String): String {
        val b = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        return b.joinToString("") { "%02x".format(it) }
    }

    fun parse(body: String, fallbackMillis: Long): Txn? {
        for (p in PATTERNS) {
            val m = p.re.find(body) ?: continue
            val f = p.build(m)
            val epoch = epochOf(f.date, f.time, fallbackMillis)
            val kindFull = p.kind + (f.via?.let { " · $it" } ?: "")
            val id = sha("${p.bank}|${f.source}|${f.amount}|${f.date}|${f.time}|${f.merchant}")
            return Txn(
                id = id,
                bank = p.bank,
                kind = kindFull,
                sourceType = p.sourceType,
                source = f.source,
                amount = num(f.amount),
                dir = f.dir,
                merchant = f.merchant,
                epochMillis = epoch,
                hasTime = f.time != null,
                bal = f.bal?.let { num(it) }
            )
        }
        return null
    }
}
