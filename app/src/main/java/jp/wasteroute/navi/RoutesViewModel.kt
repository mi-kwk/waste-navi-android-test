package jp.wasteroute.navi

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import jp.wasteroute.navi.route.ImportDecision
import jp.wasteroute.navi.route.RoutePlan
import jp.wasteroute.navi.route.RouteRepository
import jp.wasteroute.navi.route.RouteSchema
import jp.wasteroute.navi.route.RouteValidator
import jp.wasteroute.navi.route.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 取込の途中経過として画面に出すダイアログ */
sealed interface ImportDialog {
    /** 検証に失敗（既存データは変更しない） */
    data class Errors(val fileName: String, val errors: List<String>) : ImportDialog
    /** 検証は通ったので、追加・更新・変更なしを確認する */
    data class Confirm(val fileName: String, val decision: ImportDecision) : ImportDialog
    /** 取込の結果 */
    data class Done(val message: String) : ImportDialog
}

data class UiState(
    val routes: List<RoutePlan> = emptyList(),
    val importDialog: ImportDialog? = null,
    val busy: Boolean = false,
)

class RoutesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RouteRepository(app.filesDir)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val routes = withContext(Dispatchers.IO) { repo.list() }
            _state.update { it.copy(routes = routes) }
        }
    }

    /** ファイル選択画面で選ばれたJSONを読み、検証して確認ダイアログを出す */
    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val resolver = getApplication<Application>().contentResolver
            val name = withContext(Dispatchers.IO) { displayName(uri) } ?: "選択したファイル"
            val dialog = withContext(Dispatchers.IO) {
                try {
                    val bytes = resolver.openInputStream(uri)?.use { it.readNBytes(RouteSchema.MAX_FILE_BYTES + 1) }
                        ?: return@withContext ImportDialog.Errors(name, listOf("ファイルを開けませんでした"))
                    when (val result = RouteValidator.validate(bytes)) {
                        is ValidationResult.Invalid -> ImportDialog.Errors(name, result.errors)
                        is ValidationResult.Valid ->
                            ImportDialog.Confirm(name, ImportDecision.decide(result.route, repo.find(result.route.routeId)))
                    }
                } catch (e: Exception) {
                    ImportDialog.Errors(name, listOf("ファイルを読み込めませんでした: ${e.message ?: e.javaClass.simpleName}"))
                }
            }
            _state.update { it.copy(busy = false, importDialog = dialog) }
        }
    }

    /** 確認ダイアログで「取り込む」を押したとき */
    fun confirmImport() {
        val dialog = _state.value.importDialog as? ImportDialog.Confirm ?: return
        val d = dialog.decision
        if (!d.canSave) {
            dismissDialog()
            return
        }
        viewModelScope.launch {
            val message = withContext(Dispatchers.IO) {
                try {
                    repo.save(d.incoming)
                    when (d) {
                        is ImportDecision.Update -> "「${d.incoming.name}」を版${d.current.version}から版${d.incoming.version}に更新しました"
                        else -> "「${d.incoming.name}」（版${d.incoming.version}）を追加しました"
                    }
                } catch (e: Exception) {
                    "保存に失敗しました（既存のデータは変わっていません）: ${e.message}"
                }
            }
            _state.update { it.copy(importDialog = ImportDialog.Done(message)) }
            reload()
        }
    }

    fun dismissDialog() {
        _state.update { it.copy(importDialog = null) }
    }

    fun delete(routeId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.delete(routeId) }
            reload()
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()
}
