package jp.wasteroute.navi.route

import kotlinx.serialization.Serializable

/**
 * Windows版ルート作成アプリが出力するルートJSON（schemaVersion 1）。
 * 項目は Notion「Windows版ローカルWebアプリ設計書」5章に合わせる。
 */
@Serializable
data class RoutePlan(
    val schemaVersion: Int,
    val routeId: String,
    val name: String,
    val version: Int,
    val updatedAt: String,
    val routingMode: String,
    val origin: RoutePoint? = null,
    val stops: List<RouteStop>,
    val destination: RoutePoint? = null,
)

/** 出発地・最終目的地 */
@Serializable
data class RoutePoint(
    val pointId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val pointType: String,
)

/** 訪問順の地点（sequence は 1 始まりの連番） */
@Serializable
data class RouteStop(
    val pointId: String,
    val sequence: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val pointType: String,
)

/** 地点の種類（Windows版と同じ値） */
enum class PointType(val label: String) {
    COLLECTION_SITE("集積所"),
    INCINERATOR("清掃工場"),
    DEPOT("車庫");

    companion object {
        fun labelOf(value: String): String = entries.firstOrNull { it.name == value }?.label ?: value
    }
}

object RouteSchema {
    /** このアプリが読み込めるスキーマ版 */
    val SUPPORTED_SCHEMA_VERSIONS = setOf(1)
    const val ROUTING_MODE_WAYPOINTS_ONLY = "WAYPOINTS_ONLY"
    const val ROUTING_MODE_ROUTE_GEOMETRY_INCLUDED = "ROUTE_GEOMETRY_INCLUDED"
    /** 読み込むファイルの上限（誤って大きなファイルを選んだときの保護） */
    const val MAX_FILE_BYTES = 5 * 1024 * 1024
}
