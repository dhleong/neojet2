package org.neojet.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

val logger: Logger = Logger.getLogger("NeoJet:coroutines")

enum class On {
    BG,
    UI
}

/**
 * @author dhleong
 */
inline fun corun(on: On = On.BG, crossinline block: suspend () -> Unit): Job {
    val context: CoroutineContext = when (on) {
        On.BG -> Dispatchers.IO
        On.UI -> Dispatchers.Main
    }
    return GlobalScope.launch(context) {
        try {
            block()
        } catch (e: Throwable) {
            logger.warning("Unexpected Error (${e.javaClass}): ${e.toStringWithStack()}")
        }
    }
}

fun Throwable.toStringWithStack(): String =
    StringWriter().also {
        printStackTrace(PrintWriter(it))
    }.toString()
