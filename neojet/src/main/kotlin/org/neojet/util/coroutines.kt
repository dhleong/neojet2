package org.neojet.util

import kotlinx.coroutines.*
import org.neojet.NJCore
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
inline fun corun(
    on: On = On.BG,
    daemon: Boolean = false,
    crossinline block: suspend () -> Unit
): Job {
    val context: CoroutineContext = when (on) {
        On.BG -> Dispatchers.IO
        On.UI -> Dispatchers.Main
    }

    if (NJCore.isTestMode && !daemon) {
        runBlocking(context) {
            safely(block)
        }
        return Job()
    } else if (NJCore.isTestMode) {
        return GlobalScope.launch(Dispatchers.IO) { safely(block) }
//    } else if (NJCore.isTestMode) {
//        return Job()
    }

    return GlobalScope.launch(context) {
        safely(block)
    }
}

suspend inline fun safely(crossinline block: suspend () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        logger.warning("Unexpected Error (${e.javaClass}): ${e.toStringWithStack()}")
    }
}

fun Throwable.toStringWithStack(): String =
    StringWriter().also {
        printStackTrace(PrintWriter(it))
    }.toString()
