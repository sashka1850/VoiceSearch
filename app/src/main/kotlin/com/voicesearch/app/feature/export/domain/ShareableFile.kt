package com.voicesearch.app.feature.export.domain

import android.net.Uri

/** Bag carrying everything `Intent.ACTION_SEND` needs. */
data class ShareableFile(
    val uri: Uri,
    val mimeType: String,
    val displayName: String,
)
