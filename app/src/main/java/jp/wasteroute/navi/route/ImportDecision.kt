package jp.wasteroute.navi.route

/**
 * 検証済みルートを端末内の既存データと比べた結果（設計書6章の手順4〜5）。
 * - 新しいルートID → 追加
 * - 既存より大きい版番号 → 更新（確認してから置き換える）
 * - 同じ版番号 → 取り込み済みのため変更しない
 * - 既存より小さい版番号 → 古いファイルのため変更しない
 */
sealed interface ImportDecision {
    val incoming: RoutePlan

    data class AddNew(override val incoming: RoutePlan) : ImportDecision
    data class Update(override val incoming: RoutePlan, val current: RoutePlan) : ImportDecision
    data class SameVersion(override val incoming: RoutePlan, val current: RoutePlan) : ImportDecision
    data class OlderVersion(override val incoming: RoutePlan, val current: RoutePlan) : ImportDecision

    /** 保存してよいか */
    val canSave: Boolean get() = this is AddNew || this is Update

    companion object {
        fun decide(incoming: RoutePlan, current: RoutePlan?): ImportDecision = when {
            current == null -> AddNew(incoming)
            incoming.version > current.version -> Update(incoming, current)
            incoming.version == current.version -> SameVersion(incoming, current)
            else -> OlderVersion(incoming, current)
        }
    }
}
