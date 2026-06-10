package com.voicesearch.core.domain.sync

/**
 * Asks the platform layer to (eventually) sync a table back to its remote.
 *
 * The actual debouncing + retry is implemented in `:app` via WorkManager.
 * The repository layer just signals "this table changed"; the trigger
 * de-duplicates / postpones / drops as it sees fit.
 *
 * Domain doesn't know about WorkManager — keeping this an interface lets
 * unit tests stub it with no-op fakes and lets `:core:data` stay framework-free.
 */
fun interface AutoSyncTrigger {
    fun requestSync(tableId: String)
}
