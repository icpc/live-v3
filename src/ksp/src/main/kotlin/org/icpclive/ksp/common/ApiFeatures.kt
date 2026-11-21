package org.icpclive.ksp.common

import com.google.devtools.ksp.buildApiFeatures

/**
 * Api features opted in by every processor in this module.
 *
 * Ksp requires the same features to be used for the whole interaction with a processor, so this
 * is the single object each processor returns from `SymbolProcessor.apiFeatures` and passes to
 * `KSNode.validate`.
 */
val enabledApiFeatures = buildApiFeatures {
    enableBackingFields = true
    enableContextParameters = true
}
