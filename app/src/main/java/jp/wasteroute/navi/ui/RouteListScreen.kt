package jp.wasteroute.navi.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.wasteroute.navi.ImportDialog
import jp.wasteroute.navi.UiState
import jp.wasteroute.navi.route.ImportDecision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    state: UiState,
    onFilePicked: (Uri) -> Unit,
    onOpen: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onDismissDialog: () -> Unit,
    onInfo: () -> Unit,
) {
    // Storage Access Framework のファイル選択。USBで共有ストレージにコピーしたJSONを利用者が選ぶ
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onFilePicked(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ルート一覧（テスト版）") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = { TextButton(onClick = onInfo) { Text("情報", color = Color.White) } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Button(
                onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("JSONを読み込む") }
            if (state.busy) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            if (state.routes.isEmpty()) {
                Text(
                    "まだルートがありません。\nWindows版で出力したJSONをUSBで端末の「Download」などにコピーし、「JSONを読み込む」から選んでください。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.routes, key = { it.routeId }) { r ->
                        Card(Modifier.fillMaxWidth().clickable { onOpen(r.routeId) }) {
                            Column(Modifier.padding(12.dp)) {
                                Text(r.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "版 ${r.version}・${r.stops.size}地点・更新 ${formatDateTime(r.updatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.importDialog?.let { ImportDialogView(it, onConfirmImport, onDismissDialog) }
}

@Composable
private fun ImportDialogView(dialog: ImportDialog, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (dialog) {
        is ImportDialog.Errors -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("読み込めませんでした") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("${dialog.fileName}\n既存のルートは変更していません。", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    dialog.errors.forEach { Text("・$it", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
        )
        is ImportDialog.Confirm -> {
            val d = dialog.decision
            val r = d.incoming
            val (title, body) = when (d) {
                is ImportDecision.AddNew -> "新しいルートを追加" to "このルートを追加します。"
                is ImportDecision.Update ->
                    "ルートを更新" to "端末内の版${d.current.version}（${d.current.stops.size}地点）を、版${r.version}（${r.stops.size}地点）で置き換えます。"
                is ImportDecision.SameVersion -> "取り込み済みです" to "同じ版（版${r.version}）が端末内にあるため、変更しません。"
                is ImportDecision.OlderVersion ->
                    "古いファイルです" to "端末内の版${d.current.version}のほうが新しいため、版${r.version}では置き換えません。"
            }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(title) },
                text = {
                    Column {
                        Text("ルート名：${r.name}")
                        Text("版番号：${r.version}")
                        Text("地点数：${r.stops.size}")
                        Text("更新日時：${formatDateTime(r.updatedAt)}")
                        Spacer(Modifier.height(8.dp))
                        Text(body)
                    }
                },
                confirmButton = {
                    if (d.canSave) TextButton(onClick = onConfirm) { Text(if (d is ImportDecision.Update) "更新する" else "追加する") }
                    else TextButton(onClick = onDismiss) { Text("閉じる") }
                },
                dismissButton = { if (d.canSave) TextButton(onClick = onDismiss) { Text("やめる") } },
            )
        }
        is ImportDialog.Done -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("取込結果") },
            text = { Text(dialog.message) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
    }
}
