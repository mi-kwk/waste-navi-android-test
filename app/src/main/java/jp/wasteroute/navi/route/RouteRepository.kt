package jp.wasteroute.navi.route

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * 取り込んだルートを端末内（アプリ専用領域）に保存する。
 * テスト版はルート数が少ない前提のため、1ルート1ファイルの JSON で持つ（DBライブラリを増やさない）。
 * 書き込みは一時ファイル→置き換えで行い、途中で失敗しても既存ファイルを壊さない。
 */
class RouteRepository(baseDir: File) {

    private val dir = File(baseDir, "routes").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun list(): List<RoutePlan> =
        dir.listFiles { f -> f.isFile && f.name.endsWith(".json") }
            .orEmpty()
            .mapNotNull { f -> runCatching { json.decodeFromString<RoutePlan>(f.readText()) }.getOrNull() }
            .sortedBy { it.name }

    fun find(routeId: String): RoutePlan? {
        val f = fileOf(routeId)
        return if (f.isFile) runCatching { json.decodeFromString<RoutePlan>(f.readText()) }.getOrNull() else null
    }

    fun save(route: RoutePlan) {
        val target = fileOf(route.routeId)
        val tmp = File(dir, "${target.name}.tmp")
        tmp.writeText(json.encodeToString(route))
        if (!tmp.renameTo(target)) {
            // 既存ファイルがあると rename できない端末があるため、消してから再度置き換える
            target.delete()
            if (!tmp.renameTo(target)) {
                tmp.delete()
                throw IOException("ルートを保存できませんでした")
            }
        }
    }

    fun delete(routeId: String) {
        fileOf(routeId).delete()
    }

    private fun fileOf(routeId: String): File {
        // routeId は検証済み（英数字とハイフン）だが、念のためファイル名に使えない文字を除く
        val safe = routeId.replace(Regex("[^0-9A-Za-z-]"), "_")
        return File(dir, "$safe.json")
    }
}
