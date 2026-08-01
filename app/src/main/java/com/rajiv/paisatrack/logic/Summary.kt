package com.rajiv.paisatrack.logic

import com.rajiv.paisatrack.data.Txn
import java.time.LocalDate

/** Aggregates raw txns into the numbers the home screen and widget show. */
object Summary {

    data class Group(
        val key: String,
        val bank: String,
        val sourceType: String,
        val source: String,
        val windowStart: LocalDate,
        val windowEnd: LocalDate,
        val total: Double,
        val inWindow: List<Txn>,
        val earlier: List<Txn>,
        val stmtDay: Int?,      // cards only
        val daysLeft: Long?     // cards only
    )

    data class Result(
        val cards: List<Group>,
        val accounts: List<Group>,
        val cardDues: Double,
        val acctSpend: Double,
        val monthIncome: Double,
        val monthSpend: Double,
        val netSaved: Double
    )

    fun compute(all: List<Txn>, today: LocalDate = LocalDate.now()): Result {
        val debits = all.filter { it.dir == "DEBIT" }
        val credits = all.filter { it.dir == "CREDIT" }
        val month = Cycles.monthWindow(today)

        val byKey = debits.groupBy { "${it.bank}|${it.sourceType}|${it.source}" }
        val cards = ArrayList<Group>()
        val accounts = ArrayList<Group>()

        for ((key, txns) in byKey) {
            val first = txns.first()
            if (first.sourceType == "CARD") {
                val day = Cycles.stmtDay(first.source)
                val w = Cycles.cardCycle(day, today)
                val inW = txns.filter { Cycles.inWindow(it, w) }
                cards.add(
                    Group(key, first.bank, "CARD", first.source, w.start, w.end,
                        inW.sumOf { it.amount }, inW.sortedByDescending { it.epochMillis },
                        txns.filter { !Cycles.inWindow(it, w) }.sortedByDescending { it.epochMillis },
                        day, Cycles.daysLeft(w, today))
                )
            } else {
                val inW = txns.filter { Cycles.inWindow(it, month) }
                accounts.add(
                    Group(key, first.bank, first.sourceType, first.source, month.start, month.end,
                        inW.sumOf { it.amount }, inW.sortedByDescending { it.epochMillis },
                        txns.filter { !Cycles.inWindow(it, month) }.sortedByDescending { it.epochMillis },
                        null, null)
                )
            }
        }

        cards.sortByDescending { it.total }
        accounts.sortByDescending { it.total }

        val monthIncome = credits.filter { Cycles.inWindow(it, month) }.sumOf { it.amount }
        val monthSpend = debits.filter { Cycles.inWindow(it, month) }.sumOf { it.amount }

        return Result(
            cards = cards,
            accounts = accounts,
            cardDues = cards.sumOf { it.total },
            acctSpend = accounts.sumOf { it.total },
            monthIncome = monthIncome,
            monthSpend = monthSpend,
            netSaved = monthIncome - monthSpend
        )
    }
}
