package jp.wasteroute.navi.route

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** 検証結果。失敗時は利用者に見せる理由を並べる。 */
sealed interface ValidationResult {
    data class Valid(val route: RoutePlan) : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

/**
 * ルートJSONの検証（設計書6章の手順2）。
 * 形式・スキーマ版・必須項目・座標範囲・訪問順の一意性を確認する。
 * ここで失敗したファイルは保存しない（既存ルートを変更しない）。
 */
object RouteValidator {

    private val json = Json { ignoreUnknownKeys = true }
    private val UUID_LIKE = Regex("^[0-9a-fA-F-]{8,64}$")
    private val KNOWN_POINT_TYPES = PointType.entries.map { it.name }.toSet()

    fun validate(bytes: ByteArray): ValidationResult {
        if (bytes.size > RouteSchema.MAX_FILE_BYTES) {
            return ValidationResult.Invalid(listOf("ファイルが大きすぎます（上限 5MB）"))
        }
        return validate(bytes.toString(Charsets.UTF_8).removePrefix("﻿"))
    }

    fun validate(text: String): ValidationResult {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject
                ?: return ValidationResult.Invalid(listOf("JSONの形式が正しくありません（先頭がオブジェクトではありません）"))
        } catch (e: SerializationException) {
            return ValidationResult.Invalid(listOf("JSONとして読み込めません"))
        }

        // スキーマ版は他の項目より先に確認する（未対応の版なら中身の形が違う可能性があるため）
        val schemaVersion = root["schemaVersion"]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }
            ?: return ValidationResult.Invalid(listOf("schemaVersion がありません"))
        if (schemaVersion !in RouteSchema.SUPPORTED_SCHEMA_VERSIONS) {
            return ValidationResult.Invalid(
                listOf("未対応のスキーマ版です（ファイル: $schemaVersion、対応: ${RouteSchema.SUPPORTED_SCHEMA_VERSIONS.joinToString()}）。アプリの更新が必要です"),
            )
        }

        val route = try {
            json.decodeFromJsonElement<RoutePlan>(root)
        } catch (e: SerializationException) {
            return ValidationResult.Invalid(listOf("必須項目が足りないか、型が正しくありません"))
        } catch (e: IllegalArgumentException) {
            return ValidationResult.Invalid(listOf("必須項目が足りないか、型が正しくありません"))
        }

        val errors = mutableListOf<String>()
        if (!UUID_LIKE.matches(route.routeId)) errors += "routeId が正しくありません"
        if (route.name.isBlank()) errors += "ルート名が空です"
        if (route.version < 1) errors += "版番号は1以上である必要があります"
        try {
            OffsetDateTime.parse(route.updatedAt)
        } catch (e: DateTimeParseException) {
            errors += "updatedAt の日時形式が正しくありません"
        }
        when (route.routingMode) {
            RouteSchema.ROUTING_MODE_WAYPOINTS_ONLY -> {
                val geometry = root["routeGeometry"]
                if (geometry != null && geometry !is JsonNull) {
                    errors += "routingMode が WAYPOINTS_ONLY なのに routeGeometry が入っています"
                }
            }
            RouteSchema.ROUTING_MODE_ROUTE_GEOMETRY_INCLUDED ->
                errors += "道路経路形状を含むルート（ROUTE_GEOMETRY_INCLUDED）にはテスト版は未対応です"
            else -> errors += "未対応の routingMode です: ${route.routingMode}"
        }

        if (route.stops.isEmpty()) errors += "訪問順の地点がありません"
        val sequences = route.stops.map { it.sequence }
        if (sequences.toSet().size != sequences.size) {
            errors += "訪問順の番号（sequence）が重複しています"
        } else if (sequences.sorted() != (1..route.stops.size).toList()) {
            errors += "訪問順の番号（sequence）は1からの連番である必要があります"
        }
        route.stops.forEach { s ->
            val label = "訪問順${s.sequence}（${s.name.ifBlank { "名前なし" }}）"
            checkPoint(label, s.pointId, s.name, s.latitude, s.longitude, s.pointType, errors)
        }
        route.origin?.let { checkPoint("出発地", it.pointId, it.name, it.latitude, it.longitude, it.pointType, errors) }
        route.destination?.let { checkPoint("最終目的地", it.pointId, it.name, it.latitude, it.longitude, it.pointType, errors) }

        return if (errors.isEmpty()) ValidationResult.Valid(route) else ValidationResult.Invalid(errors)
    }

    private fun checkPoint(
        label: String, pointId: String, name: String,
        lat: Double, lng: Double, type: String, errors: MutableList<String>,
    ) {
        if (pointId.isBlank()) errors += "$label: 地点IDがありません"
        if (name.isBlank()) errors += "$label: 地点名がありません"
        if (!lat.isFinite() || lat < -90 || lat > 90) errors += "$label: 緯度が範囲外です（$lat）"
        if (!lng.isFinite() || lng < -180 || lng > 180) errors += "$label: 経度が範囲外です（$lng）"
        if (type !in KNOWN_POINT_TYPES) errors += "$label: 未対応の地点種類です（$type）"
    }
}
