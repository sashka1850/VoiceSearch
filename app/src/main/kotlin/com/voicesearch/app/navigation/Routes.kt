package com.voicesearch.app.navigation

/**
 * Route IDs. String-based for now; if the graph grows we'll switch to
 * type-safe navigation (Navigation 2.8+ `@Serializable` destinations).
 */
internal object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    /** Per-table search configuration. tableId carried as a path arg. */
    const val SETTINGS_PATTERN = "settings/{tableId}"
    const val ARG_TABLE_ID = "tableId"

    /** App-level settings: theme + about. */
    const val APP_SETTINGS = "app_settings"

    fun settings(tableId: String): String = "settings/$tableId"
}
