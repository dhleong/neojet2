package io.neovim.rpc.impl

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.JsonTokenId
import com.fasterxml.jackson.databind.JavaType


/**
* @author dhleong
*/
internal fun JsonParser.expect(type: JsonToken) {
    val tok = currentToken
    if (tok !== type) {
        throw JsonParseException(
            this,
            "Expected $type but was $tok"
        )
    }
}

fun JsonParser.expectNext(type: JsonToken) {
    nextToken()
    expect(type)
}

internal fun JsonParser.nextLong(): Long {
    nextValue()
    return valueAsLong
}

fun JsonParser.nextString(): String {
    nextValue()
    return valueAsString
}

internal inline fun <reified T> JsonParser.nextTypedValue(): T? {
    val tok = nextValue()
    return if (tok.id() == JsonTokenId.ID_END_ARRAY) {
        null
    } else readValueAs(T::class.java)
}

internal fun <T> JsonParser.nextTypedValue(type: Class<T>): T? {
    val tok = nextValue()
    return if (tok.id() == JsonTokenId.ID_END_ARRAY) {
        null
    } else codec.readValue(this, type)
}

internal fun <T> JsonParser.nextTypedValue(type: JavaType): T? {
    val tok = nextValue()
    return if (tok.id() == JsonTokenId.ID_END_ARRAY) {
        null
    } else codec.readValue(this, type)
}
