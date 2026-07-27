package me.kuku.legado.state

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

data class SettingsState(
    /** 上游 API 地址，默认 langge */
    var address: String = "https://api.langge.cf",
    /** Cookie，例如 qttoken=...; deviceId=... */
    var cookie: String = "",
    var enableErrorLog: Boolean = false,
    var textBodyFontColor: String = "",
    var textBodyFont: String = "",
    var enableShowBodyInLine: Boolean = false,
    var textBodyFontSize: Int = 0,
    var textBodyFontName: String = ""
)

@State(
    name = "Settings",
    storages = [Storage("LegadoReaderSettings.xml")]
)
@Service(Service.Level.PROJECT)
class SettingsService : PersistentStateComponent<SettingsState> {

    private var state: SettingsState = SettingsState()

    override fun getState(): SettingsState {
        return state
    }

    override fun loadState(state: SettingsState) {
        this.state = state
        if (this.state.address.isBlank()) {
            this.state.address = "https://api.langge.cf"
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(): SettingsService {
            return service()
        }
    }
}
