package jp.wasteroute.navi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import jp.wasteroute.navi.ui.InfoScreen
import jp.wasteroute.navi.ui.RouteDetailScreen
import jp.wasteroute.navi.ui.RouteListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1F2937), secondary = Color(0xFF2563EB))) {
                App()
            }
        }
    }
}

/** 画面：一覧 / 詳細（routeId） / 情報。画面数が少ないため Navigation ライブラリは使わない */
@Composable
private fun App(vm: RoutesViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var detailRouteId by rememberSaveable { mutableStateOf<String?>(null) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val detail = detailRouteId?.let { id -> state.routes.firstOrNull { it.routeId == id } }
    when {
        showInfo -> {
            BackHandler { showInfo = false }
            InfoScreen(onBack = { showInfo = false })
        }
        detail != null -> {
            BackHandler { detailRouteId = null }
            RouteDetailScreen(
                route = detail,
                onBack = { detailRouteId = null },
                onDelete = {
                    vm.delete(detail.routeId)
                    detailRouteId = null
                },
            )
        }
        else -> RouteListScreen(
            state = state,
            onFilePicked = vm::onFilePicked,
            onOpen = { detailRouteId = it },
            onConfirmImport = vm::confirmImport,
            onDismissDialog = vm::dismissDialog,
            onInfo = { showInfo = true },
        )
    }
}
