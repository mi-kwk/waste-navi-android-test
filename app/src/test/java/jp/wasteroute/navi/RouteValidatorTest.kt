package jp.wasteroute.navi

import jp.wasteroute.navi.route.ImportDecision
import jp.wasteroute.navi.route.RouteRepository
import jp.wasteroute.navi.route.RouteValidator
import jp.wasteroute.navi.route.ValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class RouteValidatorTest {

    /** リポジトリ同梱のデモファイル（Windows版の出力と同じ形） */
    private val demo: String = File("../samples/demo-route_v1.json").readText()

    private fun invalid(text: String): List<String> {
        val r = RouteValidator.validate(text)
        assertTrue("不正として扱われるはず: $r", r is ValidationResult.Invalid)
        return (r as ValidationResult.Invalid).errors
    }

    private fun valid(text: String) = (RouteValidator.validate(text) as ValidationResult.Valid).route

    @Test
    fun acceptsDemoFile() {
        val route = valid(demo)
        assertEquals("デモルート（架空）", route.name)
        assertEquals(4, route.stops.size)
        assertEquals("サンプル車庫", route.origin?.name)
        assertEquals("サンプル清掃工場", route.destination?.name)
    }

    @Test
    fun acceptsUtf8BomAndWindowsExportWithoutOptionalPoints() {
        val text = demo.replace(Regex("\"origin\": \\{[^}]*\\},"), "\"origin\": null,")
        val route = (RouteValidator.validate(("﻿" + text).toByteArray()) as ValidationResult.Valid).route
        assertNull(route.origin)
    }

    @Test
    fun rejectsBrokenJson() {
        assertTrue(invalid("{ not json")[0].contains("JSONとして読み込めません"))
        assertTrue(invalid("[1,2]")[0].contains("オブジェクトではありません"))
    }

    @Test
    fun rejectsUnsupportedSchemaVersion() {
        val errors = invalid(demo.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"))
        assertTrue(errors[0].contains("未対応のスキーマ版"))
    }

    @Test
    fun rejectsMissingRequiredField() {
        val errors = invalid(demo.replace("\"name\": \"デモルート（架空）\",", ""))
        assertTrue(errors[0].contains("必須項目"))
    }

    @Test
    fun rejectsOutOfRangeCoordinates() {
        val errors = invalid(demo.replace("\"latitude\": 35.6812", "\"latitude\": 95.0"))
        assertTrue(errors.any { it.contains("緯度が範囲外") && it.contains("訪問順1") })
    }

    @Test
    fun rejectsDuplicateSequence() {
        val errors = invalid(demo.replace("\"sequence\": 2", "\"sequence\": 1"))
        assertTrue(errors.any { it.contains("重複") })
    }

    @Test
    fun rejectsGapInSequence() {
        val errors = invalid(demo.replace("\"sequence\": 4", "\"sequence\": 7"))
        assertTrue(errors.any { it.contains("連番") })
    }

    @Test
    fun rejectsGeometryModeAndUnknownPointType() {
        assertTrue(invalid(demo.replace("WAYPOINTS_ONLY", "ROUTE_GEOMETRY_INCLUDED")).any { it.contains("未対応") })
        assertTrue(invalid(demo.replace("\"DEPOT\"", "\"OFFICE\"")).any { it.contains("未対応の地点種類") })
    }

    @Test
    fun rejectsEmptyStopsAndBadDate() {
        val noStops = demo.replace(Regex("\"stops\": \\[[\\s\\S]*?\\n  \\],"), "\"stops\": [],")
        assertTrue(invalid(noStops).any { it.contains("訪問順の地点がありません") })
        assertTrue(invalid(demo.replace("2026-11-26T09:00:00+09:00", "昨日")).any { it.contains("日時形式") })
    }

    @Test
    fun decidesAddUpdateSameOlder() {
        val v1 = valid(demo)
        val v2 = v1.copy(version = 2)
        assertTrue(ImportDecision.decide(v1, null) is ImportDecision.AddNew)
        assertTrue(ImportDecision.decide(v2, v1) is ImportDecision.Update)
        val same = ImportDecision.decide(v1, v1)
        assertTrue(same is ImportDecision.SameVersion)
        assertFalse(same.canSave)
        val older = ImportDecision.decide(v1, v2)
        assertTrue(older is ImportDecision.OlderVersion)
        assertFalse(older.canSave)
    }

    @Test
    fun repositoryPersistsAndReplaces() {
        val dir = Files.createTempDirectory("routes").toFile()
        val repo = RouteRepository(dir)
        val v1 = valid(demo)
        repo.save(v1)
        repo.save(v1.copy(version = 2, name = "更新後"))
        // 別インスタンス（＝アプリ再起動相当）でも残っている
        val reopened = RouteRepository(dir)
        assertEquals(1, reopened.list().size)
        assertEquals(2, reopened.find(v1.routeId)?.version)
        assertEquals("更新後", reopened.find(v1.routeId)?.name)
        reopened.delete(v1.routeId)
        assertTrue(RouteRepository(dir).list().isEmpty())
    }
}
