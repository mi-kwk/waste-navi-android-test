package jp.wasteroute.navi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.wasteroute.navi.route.PointType
import jp.wasteroute.navi.route.RoutePlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(route: RoutePlan, onBack: () -> Unit, onDelete: () -> Unit) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.name) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 一覧", color = Color.White) } },
                actions = { TextButton(onClick = { confirmDelete = true }) { Text("削除", color = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "版 ${route.version}・${route.stops.size}地点・更新 ${formatDateTime(route.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { MapPlaceholder(selectedName = selected?.let { route.stops[it].name }) }
            item {
                // 道路経路を持たないルートであることを明示する（設計書4章：ナビ可能と誤解させない）
                Notice("このルートは地点と訪問順のみです。道路に沿った経路・音声案内はありません。")
            }
            item { LocationPermissionCard() }
            route.origin?.let { o ->
                item { EndpointRow("出発地", o.name, PointType.labelOf(o.pointType), Color(0xFF16A34A)) }
            }
            item {
                Text("訪問順", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            }
            itemsIndexed(route.stops.sortedBy { it.sequence }) { index, s ->
                StopRow(
                    number = s.sequence,
                    name = s.name,
                    type = PointType.labelOf(s.pointType),
                    selected = selected == index,
                    onClick = { selected = index },
                )
                HorizontalDivider()
            }
            route.destination?.let { d ->
                item { EndpointRow("最終目的地", d.name, PointType.labelOf(d.pointType), Color(0xFFDC2626)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("ルートを削除") },
            text = { Text("「${route.name}」をこの端末から削除します。JSONファイルを読み込めば再度追加できます。") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("削除する") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("やめる") } },
        )
    }
}

/** 地図は Google Maps APIキー設定後に実装する（現在は保留） */
@Composable
private fun MapPlaceholder(selectedName: String?) {
    Box(
        Modifier.fillMaxWidth().height(160.dp)
            .background(Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("地図表示は準備中です", fontWeight = FontWeight.Bold)
            Text(
                "Google Maps のAPIキー設定後に、現在地と地点のピン（番号付き）を表示します。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selectedName != null) {
                Spacer(Modifier.height(4.dp))
                Text("選択中：$selectedName", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))) {
        Text(text, Modifier.padding(10.dp), color = Color(0xFF92400E), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EndpointRow(label: String, name: String, type: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text("$label：", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$name（$type）")
    }
}

@Composable
private fun StopRow(number: Int, name: String, type: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) Color(0xFFDBEAFE) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp).background(Color(0xFF1F2937), CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, fontWeight = FontWeight.Medium)
            Text(type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
