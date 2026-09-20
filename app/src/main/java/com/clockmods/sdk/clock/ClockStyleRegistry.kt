package com.clockmods.sdk.clock

import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap

/** Ordered, thread-safe registry used by settings, previews, and the live clock host. */
class ClockStyleRegistry {
    private val styles = LinkedHashMap<String, ClockStyle>()
    private var fallbackId: String? = null

    @Synchronized
    fun register(style: ClockStyle?): ClockStyleRegistry {
        validateStyle(style)
        val id = style!!.getMetadata().getId()
        require(!styles.containsKey(id)) { "Clock style id is already registered: $id" }
        styles[id] = style
        if (fallbackId == null) fallbackId = id
        return this
    }

    @Synchronized
    fun registerAll(additions: Iterable<ClockStyle>?): ClockStyleRegistry {
        requireNotNull(additions) { "styles must not be null" }
        additions.forEach(::register)
        return this
    }

    @Synchronized
    fun setFallback(styleId: String?): ClockStyleRegistry {
        require(styles.containsKey(styleId)) { "Fallback must be registered first: $styleId" }
        fallbackId = styleId
        return this
    }

    @Synchronized fun find(styleId: String?): ClockStyle? = styleId?.let(styles::get)

    @Synchronized
    fun resolve(styleId: String?): ClockStyle {
        ensureNotEmpty()
        return styleId?.let(styles::get) ?: styles.getValue(fallbackId!!)
    }

    @Synchronized
    fun resolveForApi(styleId: String?, apiLevel: Int): ClockStyle {
        ensureNotEmpty()
        val requested = styleId?.let(styles::get)
        if (requested != null && requested.getMetadata().supportsApi(apiLevel)) return requested
        val fallback = styles[fallbackId]
        if (fallback != null && fallback.getMetadata().supportsApi(apiLevel)) return fallback
        styles.values.firstOrNull { it.getMetadata().supportsApi(apiLevel) }?.let { return it }
        throw IllegalStateException("No registered clock style supports API $apiLevel")
    }

    @Synchronized fun getFallbackId() = fallbackId
    @Synchronized fun size() = styles.size
    @Synchronized fun getStyles(): List<ClockStyle> =
        Collections.unmodifiableList(ArrayList(styles.values))

    private fun ensureNotEmpty() {
        check(styles.isNotEmpty()) { "No clock styles have been registered" }
    }

    companion object {
        private fun validateStyle(style: ClockStyle?) {
            require(style != null && style.getMetadata() != null &&
                style.getThemeTokens() != null && style.getRenderer() != null
            ) { "ClockStyle, metadata, theme tokens, and renderer must not be null" }
        }
    }
}
