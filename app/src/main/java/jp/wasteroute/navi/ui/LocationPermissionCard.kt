package jp.wasteroute.navi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private enum class LocationAccess { NONE, APPROXIMATE, PRECISE }

private fun accessOf(context: Context): LocationAccess = when {
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED -> LocationAccess.PRECISE
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED -> LocationAccess.APPROXIMATE
    else -> LocationAccess.NONE
}

/** 最後に取得された位置（前景でのみ使用。権限がなければ null） */
@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: Context): Location? {
    if (accessOf(context) == LocationAccess.NONE) return null
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
        .maxByOrNull { it.time }
}

/**
 * 位置情報の実行時権限（設計書3章）。許可・拒否の状態と、拒否時の案内を表示する。
 * 地図上の現在地表示は APIキー設定後に実装するため、ここでは権限と最後の測位結果のみ扱う。
 */
@Composable
fun LocationPermissionCard() {
    val context = LocalContext.current
    var access by remember { mutableStateOf(accessOf(context)) }
    var denied by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(lastKnownLocation(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        access = accessOf(context)
        denied = result.values.none { it }
        location = lastKnownLocation(context)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("現在地（位置情報）", fontWeight = FontWeight.Bold)
            when (access) {
                LocationAccess.PRECISE, LocationAccess.APPROXIMATE -> {
                    Text(if (access == LocationAccess.PRECISE) "許可されています（正確な位置）" else "許可されています（おおよその位置）")
                    Text(
                        location?.let { "最後に取得した位置：%.5f, %.5f".format(it.latitude, it.longitude) }
                            ?: "まだ位置を取得していません（地図表示の実装後に現在地を表示します）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LocationAccess.NONE -> {
                    Text(
                        if (denied) "位置情報が許可されていません。現在地は表示できませんが、地点と訪問順の確認は使えます。"
                        else "現在地の表示に位置情報を使います（アプリを表示している間だけ）。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = {
                            launcher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        }) { Text("許可する") }
                        if (denied) {
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                // 「今後表示しない」で拒否された場合はアプリ設定から変更してもらう
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }) { Text("設定を開く") }
                        }
                    }
                }
            }
        }
    }
}
