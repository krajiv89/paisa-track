package com.rajiv.paisatrack

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rajiv.paisatrack.ui.AddScreen
import com.rajiv.paisatrack.ui.DetailScreen
import com.rajiv.paisatrack.ui.HomeScreen
import com.rajiv.paisatrack.ui.MainViewModel
import com.rajiv.paisatrack.notify.DueReminder

class MainActivity : ComponentActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants[Manifest.permission.READ_SMS] == true) {
                (viewModelStore).let { }  // no-op; VM handles import below
            }
            onPermsSettled()
        }

    private var vmRef: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DueReminder.schedule(this)

        setContent {
            MaterialTheme {
                val vm: MainViewModel = viewModel()
                vmRef = vm
                val summary by vm.summary.collectAsState()

                var route by remember { mutableStateOf<String>("home") }

                LaunchedEffect(Unit) {
                    vm.reload()
                    requestPerms()
                }

                when {
                    route == "home" -> HomeScreen(
                        s = summary,
                        onOpen = { key -> route = "detail:$key" },
                        onAdd = { route = "add" }
                    )
                    route == "add" -> AddScreen(
                        onBack = { route = "home" },
                        onSave = { p -> vm.addManual(p); route = "home" }
                    )
                    route.startsWith("detail:") -> {
                        val key = route.removePrefix("detail:")
                        val g = vm.groupByKey(key)
                        if (g == null) route = "home"
                        else DetailScreen(g = g, onBack = { route = "home" })
                    }
                }
            }
        }
    }

    private fun requestPerms() {
        val perms = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(perms.toTypedArray())
    }

    private fun onPermsSettled() {
        vmRef?.importInbox()
    }
}
