package com.voicesearch.app.navigation

/**
 * Route IDs. String-based for now; if the graph grows we'll switch to
 * type-safe navigation (Navigation 2.8+ `@Serializable` destinations).
 */
internal object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val SETTINGS = "settings"
}
