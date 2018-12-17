package io.neovim.events.params

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author dhleong
 */
data class ModeInfo(
    val cursorShape: CursorShape = CursorShape.BLOCK,
    val cellPercentage: Int = 100,

    @JsonProperty("blinkoff")
    val blinkOff: Int = 0,
    @JsonProperty("blinkon")
    val blinkOn: Int = 0,
    @JsonProperty("blinkwait")
    val blinkWait: Int = 0,

    @JsonProperty("hl_id")
    val highlightGroupId: Int = 0,
    @JsonProperty("hl_lm")
    val highlightGroupIdLangMap: Int = 0,

    val shortName: String = "",
    val name: String = "",

    val mouseShape: Int = 0
) {
    val isInsert: Boolean
        get() = shortName == "i"

    val isNormal: Boolean
        get() = shortName == "n"
}

enum class CursorShape {
    @JsonProperty("block") BLOCK,
    @JsonProperty("horizontal") HORIZONTAL,
    @JsonProperty("vertical") VERTICAL
}