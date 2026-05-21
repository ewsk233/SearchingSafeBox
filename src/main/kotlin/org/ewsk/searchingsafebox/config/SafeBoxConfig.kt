package org.ewsk.searchingsafebox.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.library.configuration.ConfigurationSection

object SafeBoxConfigSource {
    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration
}

class SafeBoxConfig(
    private val source: () -> Configuration
) {
    @Volatile
    var debug: Boolean = false
        private set

    @Volatile
    private var rules: Map<String, SafeBoxRule> = emptyMap()

    fun reload() {
        val config = source()
        debug = config.getBoolean("settings.debug", false)
        val root = config.getConfigurationSection("safeboxes")
        rules = root?.getKeys(false).orEmpty()
            .mapNotNull { nodeTypeId ->
                val section = root?.getConfigurationSection(nodeTypeId) ?: return@mapNotNull null
                nodeTypeId to parseRule(nodeTypeId, section)
            }
            .toMap()
    }

    fun getRule(nodeTypeId: String): SafeBoxRule? {
        return rules[nodeTypeId]
    }

    fun allRules(): List<SafeBoxRule> {
        return rules.values.sortedBy { it.nodeTypeId }
    }

    private fun parseRule(nodeTypeId: String, section: ConfigurationSection): SafeBoxRule {
        return SafeBoxRule(
            nodeTypeId = nodeTypeId,
            title = section.getString("title", "&c保险箱破译") ?: "&c保险箱破译",
            enabled = section.getBoolean("enabled", true),
            unlockUi = section.getString("unlock-ui", section.getString("unlockUi", "default") ?: "default") ?: "default",
            autoUnlock = section.getBoolean("auto-unlock", section.getBoolean("autoUnlock", false)),
            autoUnlockSeconds = section.getInt("auto-unlock-seconds", section.getInt("autoUnlockSeconds", 0)).coerceAtLeast(0),
            openImmediatelyOnSuccess = section.getBoolean(
                "open-immediately-on-success",
                section.getBoolean("openImmediatelyOnSuccess", true)
            ),
            unlockTimeoutSeconds = section.getInt(
                "unlock-timeout-seconds",
                section.getInt("unlockTimeoutSeconds", 60)
            ).coerceAtLeast(1),
            consumeOnFail = section.getBoolean("consume-on-fail", section.getBoolean("consumeOnFail", false)),
            cooldownSeconds = section.getInt("cooldown-seconds", section.getInt("cooldownSeconds", 0)).coerceAtLeast(0),
            permission = section.getString("permission")?.takeIf { it.isNotBlank() },
            messages = parseMessages(section.getConfigurationSection("messages"))
        )
    }

    private fun parseMessages(section: ConfigurationSection?): Map<String, String> {
        if (section == null) {
            return emptyMap()
        }
        return section.getKeys(false).associateWith { key -> section.getString(key).orEmpty() }
    }
}
