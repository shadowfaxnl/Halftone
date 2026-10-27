package app.halftone.desktop.player

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * Een dunne koppeling met libmpv (de bibliotheek achter de mpv-speler).
 * libmpv speelt vrijwel elk formaat, gapless, met netwerkcache, en is op elke distributie te krijgen.
 */
@Suppress("FunctionName")
internal interface MpvLib : Library {
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_observe_property(ctx: Pointer, replyUserdata: Long, name: String, format: Int): Int
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer?
    fun mpv_wakeup(ctx: Pointer)
}

@Suppress("FunctionName")
internal interface CLib : Library {
    fun setlocale(category: Int, locale: String): String?
}

/** Eén gebeurtenis van mpv, vertaald naar Kotlin. */
sealed interface MpvEvent {
    data class Property(val name: String, val value: Any?) : MpvEvent
    data class EndFile(val reason: Int) : MpvEvent
    data object FileLoaded : MpvEvent
    data object Shutdown : MpvEvent
}

class Mpv private constructor(
    private val lib: MpvLib,
    private val ctx: Pointer,
) {
    @Volatile
    private var running = true

    fun command(vararg args: String): Int = lib.mpv_command(ctx, arrayOf(*args, null))

    fun set(name: String, value: String): Int = lib.mpv_set_property_string(ctx, name, value)

    fun observe(name: String, format: Int) {
        lib.mpv_observe_property(ctx, 0L, name, format)
    }

    /** Leest gebeurtenissen op een eigen thread en geeft ze door. */
    fun startEventLoop(onEvent: (MpvEvent) -> Unit) {
        Thread({
            while (running) {
                val ev = lib.mpv_wait_event(ctx, 0.5) ?: continue
                val id = ev.getInt(0)
                val parsed: MpvEvent? = when (id) {
                    EVENT_SHUTDOWN -> MpvEvent.Shutdown
                    EVENT_FILE_LOADED -> MpvEvent.FileLoaded
                    EVENT_END_FILE -> {
                        val data = ev.getPointer(DATA_OFFSET)
                        MpvEvent.EndFile(data?.getInt(0) ?: -1)
                    }
                    EVENT_PROPERTY_CHANGE -> {
                        val prop = ev.getPointer(DATA_OFFSET)
                        if (prop == null) {
                            null
                        } else {
                            val name = prop.getPointer(0)?.getString(0)
                            val format = prop.getInt(Native.POINTER_SIZE.toLong())
                            val valuePtr = prop.getPointer(Native.POINTER_SIZE.toLong() * 2)
                            val value: Any? = if (valuePtr == null) {
                                null
                            } else {
                                when (format) {
                                    FORMAT_FLAG -> valuePtr.getInt(0) != 0
                                    FORMAT_INT64 -> valuePtr.getLong(0)
                                    FORMAT_DOUBLE -> valuePtr.getDouble(0)
                                    FORMAT_STRING -> valuePtr.getPointer(0)?.getString(0)
                                    else -> null
                                }
                            }
                            name?.let { MpvEvent.Property(it, value) }
                        }
                    }
                    else -> null
                }
                if (parsed != null) {
                    runCatching { onEvent(parsed) }
                    if (parsed == MpvEvent.Shutdown) running = false
                }
            }
        }, "halftone-mpv-events").apply { isDaemon = true }.start()
    }

    fun destroy() {
        running = false
        runCatching { lib.mpv_wakeup(ctx) }
        runCatching { lib.mpv_terminate_destroy(ctx) }
    }

    companion object {
        const val FORMAT_STRING = 1
        const val FORMAT_FLAG = 3
        const val FORMAT_INT64 = 4
        const val FORMAT_DOUBLE = 5

        private const val EVENT_SHUTDOWN = 1
        private const val EVENT_END_FILE = 7
        private const val EVENT_FILE_LOADED = 8
        private const val EVENT_PROPERTY_CHANGE = 22

        /** mpv_event: int event_id; int error; uint64 reply_userdata; void *data */
        private const val DATA_OFFSET = 16L

        private const val LC_NUMERIC = 1

        /**
         * Maakt een mpv-speler voor alleen geluid. Geeft null als libmpv niet te laden is;
         * de app toont dan uitleg over het installeren.
         */
        fun create(clientName: String): Mpv? {
            val lib = listOf("mpv", "libmpv.so.2", "libmpv.so.1", "libmpv.so").firstNotNullOfOrNull { name ->
                runCatching { Native.load(name, MpvLib::class.java) }.getOrNull()
            } ?: return null

            // libmpv weigert te starten als getallen in een andere notatie (1,5 in plaats van 1.5) worden geschreven.
            runCatching { Native.load("c", CLib::class.java).setlocale(LC_NUMERIC, "C") }

            val ctx = lib.mpv_create() ?: return null
            val options = linkedMapOf(
                "vo" to "null",
                "vid" to "no",
                "audio-display" to "no",
                "idle" to "yes",
                "terminal" to "no",
                "input-default-bindings" to "no",
                "input-vo-keyboard" to "no",
                "osc" to "no",
                "ytdl" to "no",
                "gapless-audio" to "weak",
                "prefetch-playlist" to "yes",
                "cache" to "yes",
                "demuxer-max-bytes" to "64MiB",
                "audio-client-name" to clientName,
                "title" to clientName,
                "msg-level" to "all=error",
            )
            options.forEach { (k, v) -> lib.mpv_set_option_string(ctx, k, v) }
            if (lib.mpv_initialize(ctx) < 0) {
                runCatching { lib.mpv_terminate_destroy(ctx) }
                return null
            }
            return Mpv(lib, ctx)
        }
    }
}
