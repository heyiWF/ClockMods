package com.clockmods.sdk.clock

import java.util.Locale

/** Stable identity and compatibility information for a clock style. */
class ClockStyleMetadata(
    id: String?,
    name: String?,
    description: String?,
    kind: Kind?,
    capabilities: ClockStyleCapabilities?,
    version: Int,
    minApi: Int,
) {
    enum class Kind { ANALOG, DIGITAL, HYBRID }

    private val id = requireStyleId(id)
    private val name = requireText(name, "name")
    private val description = requireText(description, "description")
    private val kind = requireNotNull(kind) { "kind must not be null" }
    private val capabilities = requireNotNull(capabilities) { "capabilities must not be null" }
    private val version = version.also { require(it >= 1) { "version must be at least 1" } }
    private val minApi = minApi.also { require(it >= 1) { "minApi must be at least 1" } }

    fun getId() = id
    fun getName() = name
    fun getDescription() = description
    fun getKind() = kind
    fun getCapabilities() = capabilities
    fun getVersion() = version
    fun getMinApi() = minApi
    fun supportsApi(apiLevel: Int) = apiLevel >= minApi

    override fun equals(other: Any?): Boolean = other is ClockStyleMetadata &&
        id == other.id && name == other.name && description == other.description &&
        kind == other.kind && capabilities == other.capabilities &&
        version == other.version && minApi == other.minApi

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + version
        return 31 * result + minApi
    }

    override fun toString() = "$id@$version ($kind)"

    private companion object {
        fun requireText(value: String?, label: String) = value?.trim().orEmpty().also {
            require(it.isNotEmpty()) { "$label must not be blank" }
        }

        fun requireStyleId(value: String?): String {
            val id = requireText(value, "id")
            require(id == id.lowercase(Locale.US) &&
                Regex("[a-z][a-z0-9]*(?:[._-][a-z0-9]+)+").matches(id)
            ) { "id must be a lowercase, namespaced identifier: $id" }
            return id
        }
    }
}
