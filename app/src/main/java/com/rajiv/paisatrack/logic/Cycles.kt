package com.rajiv.paisatrack.logic

import com.rajiv.paisatrack.data.Txn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Billing-cycle + calendar-month windowing. Cards use their statement day. */
object Cycles {

    // RuPay 8752 bills 22->22; every other card bills 15->15.
    private val STMT_DAY = mapOf("8752" to 22)
    const val DEFAULT_STMT_DAY = 15

    fun last4(source: String) = source.trimStart('X', '*')
    fun stmtDay(source: String) = STMT_DAY[last4(source)] ?: DEFAULT_STMT_DAY

    data class Window(val start: LocalDate, val end: LocalDate)

    fun localDate(epoch: Long): LocalDate =
        Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun clampDay(y: Int, m: Int, day: Int): Int {
        val len = LocalDate.of(y, m, 1).lengthOfMonth()
        return minOf(day, len)
    }

    /** Current billing cycle for a card, given today. */
    fun cardCycle(day: Int, today: LocalDate): Window {
        val startMonthDay = clampDay(today.year, today.monthValue, day)
        val start: LocalDate = if (today.dayOfMonth >= startMonthDay) {
            LocalDate.of(today.year, today.monthValue, startMonthDay)
        } else {
            val prev = today.minusMonths(1)
            LocalDate.of(prev.year, prev.monthValue, clampDay(prev.year, prev.monthValue, day))
        }
        val next = start.plusMonths(1)
        val nextStmt = LocalDate.of(next.year, next.monthValue, clampDay(next.year, next.monthValue, day))
        return Window(start, nextStmt.minusDays(1))
    }

    fun monthWindow(today: LocalDate): Window =
        Window(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))

    fun inWindow(t: Txn, w: Window): Boolean {
        val d = localDate(t.epochMillis)
        return !d.isBefore(w.start) && !d.isAfter(w.end)
    }

    fun daysLeft(w: Window, today: LocalDate): Long =
        maxOf(0, ChronoUnit.DAYS.between(today, w.end))
}
