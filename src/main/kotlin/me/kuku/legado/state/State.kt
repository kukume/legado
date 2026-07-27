package me.kuku.legado.state


object State {

    @JvmStatic
    val settings by lazy {
        SettingsService.getInstance().state
    }

}
