package io.neovim.types

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author dhleong
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class NeovimApiInfo(
    val channelId: Long,
    val apiMetadata: NeovimApiMetadata
)

data class NeovimApiMetadata(
    val version: NeovimVersion,
    val functions: List<NeovimApiFunction>,
    val uiEvents: List<NeovimApiEvent>,
    val uiOptions: List<String>,
    val types: Map<String, NeovimApiTypeInfo>
)

data class NeovimVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val apiLevel: Int,
    val apiCompatible: Int,
    val apiPrerelease: Boolean
)

// in the future we could wrap this type somehow
typealias NeovimApiType = String

interface NeovimApiCallable {
    val parameters: List<NeovimApiParameter>
    val name: String
    val since: Int
    val deprecatedSince: Int

    val isDeprecated: Boolean
        get() = deprecatedSince != -1
 }

data class NeovimApiFunction(
    override val name: String,
    override val parameters: List<NeovimApiParameter>,

    override val since: Int,
    override val deprecatedSince: Int = -1,

    @JsonProperty("method")
    val isMethod: Boolean,

    val returnType: NeovimApiType
) : NeovimApiCallable

data class NeovimApiEvent(
    override val name: String,
    override val parameters: List<NeovimApiParameter>,

    override val since: Int,
    override val deprecatedSince: Int = -1
) : NeovimApiCallable

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
data class NeovimApiParameter(
    val type: NeovimApiType,
    val name: String
)

data class NeovimApiTypeInfo(
    val id: Int,
    val prefix: String
)

