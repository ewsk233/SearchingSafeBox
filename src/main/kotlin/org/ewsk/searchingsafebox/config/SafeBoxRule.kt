package org.ewsk.searchingsafebox.config

data class SafeBoxRule(
    val nodeTypeId: String,
    val enabled: Boolean,
    val title: String,
    val unlockUi: String,
    val autoUnlock: Boolean,
    val autoUnlockSeconds: Int,
    val openImmediatelyOnSuccess: Boolean,
    val unlockTimeoutSeconds: Int,
    val consumeOnFail: Boolean,
    val cooldownSeconds: Int,
    val permission: String?,
    val messages: Map<String, String>
)
