package com.voicesearch.core.domain.time

/**
 * Wall-clock indirection so we can inject a controllable time source in tests.
 * Production binding is [SystemClock].
 */
fun interface Clock {
    fun nowMillis(): Long
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
