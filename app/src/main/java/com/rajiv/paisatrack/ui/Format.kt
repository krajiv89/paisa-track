package com.rajiv.paisatrack.ui

import androidx.compose.ui.graphics.Color
import com.rajiv.paisatrack.logic.Cycles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val BankColor = mapOf(
    "Axis" to Color(0xFFE6317A),
    "HDFC" to Color(0xFF2B7FFF),
    "Indian Bank" to Color(0xFFFB8500),
    "Union Bank" to Color(0xFF7C4DFF),
    "Kotak" to Color(0xFFFF3B5C),
    "Cash" to Color(0xFF12B886)
)
val BankSoft = mapOf(
    "Axis" to Color(0xFFFCE4EF),
    "HDFC" to Color(0xFFE4EFFF),
    "Indian Bank" to Color(0xFFFFF0DC),
    "Union Bank" to Color(0xFFECE6FF),
    "Kotak" to Color(0xFFFFE3E8),
    "Cash" to Color(0xFFDFF6EC)
)
fun colorFor(bank: String) = BankColor[bank] ?: Color(0xFF64748B)
fun softFor(bank: String) = BankSoft[bank] ?: Color(0xFFE2E8F0)

fun inr(n: Double): String {
    val whole = n % 1.0 == 0.0
    // Indian grouping (lakh/crore) with optional 2 decimals
    val neg = n < 0
    val abs = Math.abs(n)
    val s = if (whole) formatIndian(abs.toLong().toString())
    else {
        val parts = String.format("%.2f", abs).split(".")
        formatIndian(parts[0]) + "." + parts[1]
    }
    return (if (neg) "-₹" else "₹") + s
}

private fun formatIndian(intPart: String): String {
    if (intPart.length <= 3) return intPart
    val last3 = intPart.substring(intPart.length - 3)
    var rest = intPart.substring(0, intPart.length - 3)
    val sb = StringBuilder()
    while (rest.length > 2) {
        sb.insert(0, "," + rest.substring(rest.length - 2))
        rest = rest.substring(0, rest.length - 2)
    }
    if (rest.isNotEmpty()) sb.insert(0, rest)
    return sb.toString() + "," + last3
}

private val MON = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
fun shortDate(d: LocalDate) = "${d.dayOfMonth} ${MON[d.monthValue - 1]}"

fun txnLabel(epoch: Long, hasTime: Boolean): String {
    val d = Cycles.localDate(epoch)
    val base = shortDate(d)
    if (!hasTime) return base
    val t = java.time.Instant.ofEpochMilli(epoch)
        .atZone(java.time.ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    return "$base · $t"
}
fun last4(s: String) = s.trimStart('X', '*')
