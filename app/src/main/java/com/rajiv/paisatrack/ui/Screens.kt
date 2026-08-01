package com.rajiv.paisatrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajiv.paisatrack.logic.ManualParser
import com.rajiv.paisatrack.logic.Summary

private val Bg = Color(0xFFF3F4F6)
private val Ink = Color(0xFF1F2937)
private val Sub = Color(0xFF9CA3AF)

@Composable
fun HomeScreen(s: Summary.Result, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            // Header: two numbers
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF4A1D96), Color(0xFF7C3AED), Color(0xFFDB2777))))
                    .padding(20.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    HeaderStat("CARD DUES", inr(s.cardDues), "current cycles", Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(46.dp).background(Color.White.copy(alpha = 0.18f)))
                    Spacer(Modifier.width(14.dp))
                    HeaderStat("ACCOUNT SPENDS", inr(s.acctSpend), "this month", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Divider(color = Color.White.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))
                val saved = s.netSaved
                Text(
                    "Earned ${inr(s.monthIncome)}  ·  Spent ${inr(s.monthSpend)}",
                    color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp
                )
                Text(
                    if (saved >= 0) "Saved ${inr(saved)} this month" else "Overspent ${inr(-saved)} this month",
                    color = if (saved >= 0) Color(0xFFB9F6CA) else Color(0xFFFFCDD2),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("CREDIT CARDS · BY BILLING CYCLE")
            if (s.cards.isEmpty()) EmptyHint("No card spends yet")
            s.cards.forEach { g -> GroupRow(g) { onOpen(g.key) } }

            Spacer(Modifier.height(18.dp))
            SectionLabel("ACCOUNTS & CASH · THIS MONTH")
            if (s.accounts.isEmpty()) EmptyHint("No account spends yet")
            s.accounts.forEach { g -> GroupRow(g) { onOpen(g.key) } }

            Spacer(Modifier.height(90.dp))
        }

        ExtendedFloatingActionButton(
            onClick = onAdd,
            containerColor = Color(0xFF4A1D96),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Text("+  Add cash", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun HeaderStat(label: String, value: String, sub: String, mod: Modifier) {
    Column(mod) {
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun SectionLabel(t: String) =
    Text(t, color = Sub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))

@Composable
private fun EmptyHint(t: String) =
    Text(t, color = Sub, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))

@Composable
private fun GroupRow(g: Summary.Group, onClick: () -> Unit) {
    val c = colorFor(g.bank)
    val zero = g.total == 0.0
    Surface(
        color = Color.White, shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onClick() }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(if (zero) Color(0xFFCBD5E1) else c))
                Spacer(Modifier.width(8.dp))
                Text("${g.bank} ${if (g.sourceType == "CARD") "Card" else if (g.sourceType == "CASH") "" else "A/c"}",
                    color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (g.sourceType != "CASH") {
                    Spacer(Modifier.width(6.dp))
                    Text("···${last4(g.source)}", color = Sub, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(inr(g.total), color = if (zero) Color(0xFF94A3B8) else c, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("  ›", color = Color(0xFFD1D5DB), fontSize = 15.sp)
            }
            if (!zero) {
                Spacer(Modifier.height(8.dp))
                val frac = 1f  // bar relative to itself; simple full bar tinted
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF1F1F4))) {
                    Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c))
                }
            }
            Spacer(Modifier.height(6.dp))
            val meta = when {
                g.sourceType == "CARD" ->
                    "${shortDate(g.windowStart)}–${shortDate(g.windowEnd)} · ${g.daysLeft} days to statement"
                zero && g.earlier.isNotEmpty() ->
                    "No spend this month · last ${shortDate(com.rajiv.paisatrack.logic.Cycles.localDate(g.earlier.first().epochMillis))}"
                else -> "${g.inWindow.size} txn${if (g.inWindow.size != 1) "s" else ""} this month"
            }
            Text(meta, color = Sub, fontSize = 10.sp)
        }
    }
}

@Composable
fun DetailScreen(g: Summary.Group, onBack: () -> Unit) {
    val c = colorFor(g.bank)
    val isCard = g.sourceType == "CARD"
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(14.dp)) {
        Text("‹ Back", color = Sub, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 6.dp))
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c).padding(18.dp)) {
            val head = "${g.bank} ${if (isCard) "Card" else if (g.sourceType == "CASH") "" else "A/c"} ···${last4(g.source)}"
            Text(head.trim(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, letterSpacing = 1.sp)
            Text(inr(g.total), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
            val sub = if (isCard)
                "Cycle ${shortDate(g.windowStart)}–${shortDate(g.windowEnd)} · statement ${g.stmtDay} · ${g.daysLeft} days left"
            else "This month"
            Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (g.inWindow.isEmpty())
            Text("No spend in this ${if (isCard) "cycle" else "month"} yet.", color = Sub, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 10.dp))
        g.inWindow.forEach { TxnRow(it) }
        if (g.earlier.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionLabel("EARLIER · OUTSIDE CURRENT ${if (isCard) "CYCLE" else "MONTH"}")
            g.earlier.forEach { TxnRow(it, faded = true) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TxnRow(t: com.rajiv.paisatrack.data.Txn, faded: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    val c = colorFor(t.bank)
    Surface(color = Color.White, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { open = !open }
            .then(if (faded) Modifier.background(Color.Transparent) else Modifier)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.merchant, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(txnLabel(t.epochMillis, t.hasTime) + if (!t.hasTime) " · time from SMS" else "",
                        color = Sub, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(inr(t.amount), color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Box(Modifier.padding(top = 3.dp).clip(RoundedCornerShape(8.dp)).background(softFor(t.bank))
                        .padding(horizontal = 7.dp, vertical = 1.dp)) {
                        Text(t.bank + if (t.sourceType == "CARD") " ····${last4(t.source)}" else "",
                            color = c, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (open) {
                Spacer(Modifier.height(8.dp))
                Divider(color = Color(0xFFF0F0F2))
                Spacer(Modifier.height(6.dp))
                DetailLine("Type", t.kind)
                DetailLine("Paid to", t.merchant)
                DetailLine("Amount", inr(t.amount))
                DetailLine(if (t.sourceType == "CARD") "Card" else "Account", t.source)
                DetailLine("Date & time", txnLabel(t.epochMillis, t.hasTime))
                t.bal?.let { DetailLine(if (t.sourceType == "CARD") "Avl limit" else "Avl balance", inr(it)) }
            }
        }
    }
}

@Composable
private fun DetailLine(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, color = Sub, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(v, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddScreen(onBack: () -> Unit, onSave: (ManualParser.Preview) -> Unit) {
    var text by remember { mutableStateOf("") }
    val preview = remember(text) { ManualParser.parse(text) }
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        Text("‹ Back", color = Sub, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable { onBack() }.padding(vertical = 6.dp))
        Spacer(Modifier.height(8.dp))
        Text("Add a cash entry", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Just type it in plain English.", color = Sub, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            placeholder = { Text("e.g. I spent rs 70 in tea shop") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true
        )
        Spacer(Modifier.height(14.dp))
        if (preview != null) {
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("This will be saved as:", color = Sub, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(inr(preview.amount), color = colorFor("Cash"), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("${preview.merchant} · ${if (preview.given) "cash given" else "cash spend"} · today",
                        color = Ink, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onSave(preview) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1D96))) {
                Text("Save entry", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else if (text.isNotBlank()) {
            Text("Couldn't read an amount. Try: \"spent 70 at tea shop\" or \"gave 1000 to wife\".",
                color = Color(0xFFB91C1C), fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("Examples", color = Sub, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        listOf("I spent rs 70 in tea shop", "I gave rs 1000 to wife", "paid 250 for auto", "spent 1.5k at groceries")
            .forEach { ex ->
                Text("•  $ex", color = Ink, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 3.dp).clickable { text = ex })
            }
    }
}
