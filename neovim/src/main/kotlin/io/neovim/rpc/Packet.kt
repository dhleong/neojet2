package io.neovim.rpc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonValue

/**
 * @author dhleong
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
interface Packet {

    enum class Type {
        REQUEST,
        RESPONSE,
        NOTIFICATION;

        @JsonValue
        fun toValue() = ordinal

        companion object {
            private val VALUES = values()

            @JsonCreator
            @JvmStatic
            fun create(ordinal: Int) = VALUES[ordinal]
        }
    }

    val type: Type
}