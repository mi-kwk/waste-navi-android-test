package jp.wasteroute.navi.ui

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DISPLAY = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

/** ISO-8601 の日時を端末のタイムゾーンで表示する（解析できなければそのまま） */
fun formatDateTime(iso: String): String =
    runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DISPLAY) }
        .getOrDefault(iso)
