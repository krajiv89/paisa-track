package com.rajiv.paisatrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rajiv.paisatrack.data.Store
import com.rajiv.paisatrack.data.Txn
import com.rajiv.paisatrack.logic.ManualParser
import com.rajiv.paisatrack.logic.Summary
import com.rajiv.paisatrack.sms.SmsImporter
import com.rajiv.paisatrack.widget.SpendWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private var txns: List<Txn> = emptyList()

    private val _summary = MutableStateFlow(Summary.compute(emptyList()))
    val summary: StateFlow<Summary.Result> = _summary.asStateFlow()

    fun reload() {
        txns = Store.load(getApplication())
        _summary.value = Summary.compute(txns)
    }

    fun importInbox() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { SmsImporter.importInbox(getApplication()) }
            reload()
            SpendWidget.refresh(getApplication())
        }
    }

    fun addManual(preview: ManualParser.Preview) {
        viewModelScope.launch {
            val t = ManualParser.toTxn(preview)
            withContext(Dispatchers.IO) { Store.addUnique(getApplication(), listOf(t)) }
            reload()
            SpendWidget.refresh(getApplication())
        }
    }

    fun groupByKey(key: String): Summary.Group? {
        val s = _summary.value
        return (s.cards + s.accounts).find { it.key == key }
    }
}
