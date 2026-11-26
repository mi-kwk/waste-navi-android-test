# waste-navi-android-test

ごみ収集車（集積所巡回）向けカーナビの **テスト版Androidアプリ** です。
Windows版ルート作成アプリが出力したルートJSONを、USBで端末にコピーして取り込み、集配地点と訪問順を確認できるかを検証します。

> これは検証用のテスト版です。業務での利用は想定していません。
> 公開リポジトリのため、実在の集配データ・APIキー・署名鍵は含めません（デモデータはすべて架空です）。

## できること（v0.1.0-test）

- **JSONの取り込み**：Android のファイル選択画面（Storage Access Framework）から JSON を選ぶ
- **検証**：スキーマ版・必須項目・座標範囲・訪問順の重複/連番・地点種類・日時形式・ファイルサイズ（5MB）
  - 不正なファイルは理由を表示し、既存ルートは変更しない
- **追加・更新ルール**（同じ routeId で比べる）
  - 新しいルート → 確認して追加
  - 版番号が大きい → 端末内の版と比べて確認してから置き換え
  - 同じ版 / 古い版 → 変更しない（理由を表示）
- **ルート一覧・詳細**：ルート名・版・地点数・更新日時、出発地／訪問順（番号付き）／最終目的地
  - 地点と訪問順のみのルートであること（道路経路・音声案内はない）を明示
- **位置情報の権限**：前景のみで要求。許可・拒否の状態表示、拒否時の案内と「設定を開く」
- **情報画面**：テスト版であること、アプリの版、対応スキーマ版、地図・位置情報・圏外時の説明
- 取り込んだルートはアプリ内（端末内）に保存され、再起動後も残る。圏外でも一覧・訪問順は確認できる

### 保留中

- **地図表示（Maps SDK for Android）**：Google Maps APIキーの準備後に実装します。現在は詳細画面に「地図表示は準備中」と表示します。
- 道路経路形状（`ROUTE_GEOMETRY_INCLUDED`）、Navigation SDK による案内：初期範囲外です。

## 対応するJSON

Windows版（[waste-route-planner-windows]）の出力 `schemaVersion: 1`、`routingMode: WAYPOINTS_ONLY`。
サンプル：[`samples/demo-route_v1.json`](samples/demo-route_v1.json)（架空のデータ）

```json
{
  "schemaVersion": 1,
  "routeId": "UUID",
  "name": "ルート名",
  "version": 1,
  "updatedAt": "2026-11-26T09:00:00+09:00",
  "routingMode": "WAYPOINTS_ONLY",
  "origin": { "pointId": "...", "name": "...", "latitude": 35.68, "longitude": 139.76, "pointType": "DEPOT" },
  "stops": [
    { "pointId": "...", "sequence": 1, "name": "...", "latitude": 35.68, "longitude": 139.76, "pointType": "COLLECTION_SITE" }
  ],
  "destination": null,
  "routeGeometry": null
}
```

`pointType`：`COLLECTION_SITE`（集積所）／`INCINERATOR`（清掃工場）／`DEPOT`（車庫）

## 構成

| 項目 | 内容 |
| --- | --- |
| 言語・UI | Kotlin 2.2 / Jetpack Compose（Material 3） |
| 最低対応 | Android 8.0（API 26）、target API 36 |
| アプリID | `jp.wasteroute.navi.test`（実務版とは別） |
| JSON | kotlinx.serialization |
| 端末内保存 | アプリ専用領域に1ルート1ファイルのJSON（一時ファイル→置き換えで保存） |
| ビルド | Gradle 8.13 / AGP 8.13 / JDK 17 以上 |

## ビルドと実行

```powershell
# JDK（Android Studio 付属の jbr でよい）
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest   # 単体テスト
.\gradlew.bat :app:assembleDebug        # APK: app\build\outputs\apk\debug\app-debug.apk
```

`local.properties` に Android SDK の場所（`sdk.dir=...`）が必要です（Android Studio で開くと自動で作られます）。

### 端末での確認手順

1. APK をインストールする
2. Windows版で出力した JSON（または `samples/demo-route_v1.json`）を USB で端末の `Download` にコピーする
3. アプリの「JSONを読み込む」からファイルを選び、確認画面で「追加する」

## 設計

Notion「テスト版Androidカーナビアプリ設計書」（v0.1）に従います。設計書10章の未決事項は、実装時に次の前提を置いています（レビューで変更可）。

- 最低対応OSは Android 8.0、Kotlin + Jetpack Compose を採用
- JSONは Windows版の schemaVersion 1 をそのまま使う。版番号は大きいほうを新しいとみなし、同じ版・古い版では置き換えない
- 出発地・最終目的地は JSON の値を表示する（現在地を起点にする処理は地図実装時に検討）
- 道路経路は扱わない（WAYPOINTS_ONLY のみ対応）
- 圏外時に保証するのは、取り込み済みルートの一覧・訪問順の表示のみ
- Navigation SDK はテスト版の範囲外
- 配布はデバッグ署名の APK を手動インストール（署名鍵は扱わない）

[waste-route-planner-windows]: https://github.com/mi-kwk/waste-route-planner-windows
