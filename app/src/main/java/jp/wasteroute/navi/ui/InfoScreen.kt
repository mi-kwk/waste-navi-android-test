package jp.wasteroute.navi.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import jp.wasteroute.navi.BuildConfig
import jp.wasteroute.navi.route.RouteSchema

/** 設定・情報（設計書5章）：テスト版であること、版、対応スキーマ版、地図・位置情報の説明 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("情報") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 戻る", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("これはテスト版です", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("ルートデータの取り込みと表示を検証するためのアプリです。業務での利用は想定していません。")
            Spacer(Modifier.height(16.dp))
            Item("アプリの版", "${BuildConfig.VERSION_NAME}（${BuildConfig.APPLICATION_ID}）")
            Item("対応するJSONスキーマ版", RouteSchema.SUPPORTED_SCHEMA_VERSIONS.joinToString())
            Item("対応するルート形式", "地点と訪問順のみ（${RouteSchema.ROUTING_MODE_WAYPOINTS_ONLY}）")
            Item("地図", "Google Maps（Maps SDK for Android）を使う予定です。APIキー設定後に表示します。")
            Item("位置情報", "現在地の表示に使います。アプリを表示している間だけ使い、外部へ送信しません。")
            Item("データの保存", "読み込んだルートはこの端末のアプリ内にだけ保存します。")
            Item("通信圏外", "読み込み済みのルート一覧・訪問順は圏外でも確認できます。地図・道路経路・音声案内の圏外動作は保証しません。")
        }
    }
}

@Composable
private fun Item(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value)
    Spacer(Modifier.height(12.dp))
}
