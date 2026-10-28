package nl.wdhertog.takenlijst.models

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Een model dat de app eenmalig downloadt en daarna offline gebruikt. */
enum class Model(val label: String, val sizeMb: Int, val files: List<Pair<String, String>>) {
    /** Inspreken: whisper.cpp, small-model (goed in Nederlands, ~190 MB). */
    SPEECH(
        "Spraakherkenning (Whisper small)", 190,
        listOf("ggml-small-q5_1.bin" to "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small-q5_1.bin"),
    ),

    /** Handschrift: TrOCR-small (Microsoft, MIT), als ONNX. */
    HANDWRITING(
        "Handschriftherkenning (TrOCR small)", 67,
        listOf(
            "trocr-encoder.onnx" to "$TROCR/onnx/encoder_model_quantized.onnx",
            "trocr-decoder.onnx" to "$TROCR/onnx/decoder_model_quantized.onnx",
            "trocr-tokenizer.json" to "$TROCR/tokenizer.json",
        ),
    );
}

private const val TROCR = "https://huggingface.co/Xenova/trocr-small-handwritten/resolve/main"

/** Downloadt modellen. Dit is het enige waarvoor de app internet gebruikt. */
object ModelManager {
    /** Voortgang 0..1 per model dat nu gedownload wordt. */
    val progress = MutableStateFlow<Map<Model, Float>>(emptyMap())
    val error = MutableStateFlow<String?>(null)
    /** Wordt opgehoogd na een download of verwijdering, zodat de UI opnieuw kijkt wat er staat. */
    val changes = MutableStateFlow(0)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun dir(ctx: Context) = File(ctx.filesDir, "models").apply { mkdirs() }
    fun file(ctx: Context, name: String) = File(dir(ctx), name)

    fun isDownloaded(ctx: Context, model: Model) = model.files.all { (name, _) -> file(ctx, name).exists() }

    fun delete(ctx: Context, model: Model) {
        model.files.forEach { (name, _) -> file(ctx, name).delete() }
        changes.value++
    }

    fun download(ctx: Context, model: Model) {
        if (model in progress.value) return
        val app = ctx.applicationContext
        progress.value += model to 0f
        error.value = null
        scope.launch {
            try {
                val todo = model.files.filterNot { (name, _) -> file(app, name).exists() }
                todo.forEachIndexed { index, (name, url) ->
                    fetch(url, file(app, name)) { fraction ->
                        progress.value += model to (index + fraction) / todo.size
                    }
                }
            } catch (e: Exception) {
                error.value = "Download mislukt: ${e.message}"
            } finally {
                progress.value -= model
                changes.value++
            }
        }
    }

    private fun fetch(url: String, target: File, onProgress: (Float) -> Unit) {
        val part = File(target.path + ".part")
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.instanceFollowRedirects = true
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress(done.toFloat() / total)
                    }
                }
            }
            if (!part.renameTo(target)) throw IOException("bestand kon niet worden opgeslagen")
        } finally {
            part.delete()
        }
    }
}
