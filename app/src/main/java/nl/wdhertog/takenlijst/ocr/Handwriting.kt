package nl.wdhertog.takenlijst.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.models.ModelManager
import org.json.JSONObject
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.exp

/**
 * Handschrift lezen met TrOCR-small (encoder-decoder, getraind op handgeschreven regels).
 * Het model leest één regel tegelijk; de regels zelf vinden we vooraf met Tesseract of [LineFinder].
 */
class TrOcr(private val context: Context) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 4))
    }
    private val encoder = env.createSession(path("trocr-encoder.onnx"), options)
    private val decoder = env.createSession(path("trocr-decoder.onnx"), options)
    private val vocab: List<String> = loadVocab()

    private fun path(name: String) = ModelManager.file(context, name).absolutePath

    /** De Unigram-woordenlijst uit tokenizer.json, eenmalig omgezet naar één token per regel. */
    private fun loadVocab(): List<String> {
        val cache = ModelManager.file(context, "trocr-vocab.txt")
        if (cache.exists()) return cache.readLines()
        val json = JSONObject(ModelManager.file(context, "trocr-tokenizer.json").readText())
        val list = json.getJSONObject("model").getJSONArray("vocab")
        val tokens = List(list.length()) { list.getJSONArray(it).getString(0).replace("\n", " ") }
        File(cache.path + ".tmp").apply { writeText(tokens.joinToString("\n")) }.renameTo(cache)
        return tokens
    }

    /** Leest één regel; geeft de tekst en de laagste tokenzekerheid (0..1) terug. */
    fun read(line: Bitmap): OcrLine {
        val pixels = preprocess(line)
        OnnxTensor.createTensor(env, pixels, longArrayOf(1, 3, SIZE.toLong(), SIZE.toLong())).use { input ->
            encoder.run(mapOf("pixel_values" to input)).use { encoded ->
                val hidden = encoded[0] as OnnxTensor
                val ids = mutableListOf(START)
                var lowest = 1f
                repeat(MAX_TOKENS) {
                    val idTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(ids.map { it.toLong() }.toLongArray()), longArrayOf(1, ids.size.toLong()))
                    val (next, p) = idTensor.use {
                        decoder.run(mapOf("input_ids" to it, "encoder_hidden_states" to hidden), setOf("logits")).use { out ->
                            lastTokenArgmax((out[0] as OnnxTensor).floatBuffer, ids.size)
                        }
                    }
                    if (next == END) return finish(ids, lowest)
                    ids += next
                    lowest = minOf(lowest, p)
                }
                return finish(ids, lowest)
            }
        }
    }

    private fun finish(ids: List<Int>, confidence: Float): OcrLine {
        val text = ids.drop(1).filter { it > 3 }.joinToString("") { vocab.getOrElse(it) { "" } }
            .replace('▁', ' ').trim()
        return OcrLine(text, confidence * 100f)
    }

    /** Hoogste logit van de laatste positie, met de softmax-kans erbij. */
    private fun lastTokenArgmax(logits: FloatBuffer, length: Int): Pair<Int, Float> {
        val vocabSize = logits.capacity() / length
        val offset = (length - 1) * vocabSize
        var best = 0
        var max = Float.NEGATIVE_INFINITY
        for (i in 0 until vocabSize) {
            val v = logits.get(offset + i)
            if (v > max) { max = v; best = i }
        }
        var sum = 0.0
        for (i in 0 until vocabSize) sum += exp((logits.get(offset + i) - max).toDouble())
        return best to (1.0 / sum).toFloat()
    }

    /** Schalen naar 384×384 en normaliseren naar [-1, 1], zoals de DeiT-beeldverwerker van TrOCR. */
    private fun preprocess(src: Bitmap): FloatBuffer {
        val scaled = Bitmap.createScaledBitmap(src, SIZE, SIZE, true)
        val px = IntArray(SIZE * SIZE)
        scaled.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        if (scaled !== src) scaled.recycle()
        val buf = FloatBuffer.allocate(3 * SIZE * SIZE)
        val plane = SIZE * SIZE
        for (i in px.indices) {
            val c = px[i]
            buf.put(i, Color.red(c) / 127.5f - 1f)
            buf.put(plane + i, Color.green(c) / 127.5f - 1f)
            buf.put(2 * plane + i, Color.blue(c) / 127.5f - 1f)
        }
        return buf
    }

    override fun close() {
        encoder.close()
        decoder.close()
        options.close()
    }

    companion object {
        private const val SIZE = 384
        private const val START = 2
        private const val END = 2
        private const val MAX_TOKENS = 24
    }
}

/**
 * Regels vinden zonder Tesseract: horizontale banden met inkt in een zwart-wit-versie van de foto.
 * Werkt goed voor een recht gefotografeerd lijstje met één kolom.
 */
object LineFinder {
    fun find(bitmap: Bitmap): List<Rect> {
        val w = bitmap.width
        val h = bitmap.height
        val px = IntArray(w * h)
        bitmap.getPixels(px, 0, w, 0, 0, w, h)
        val gray = IntArray(px.size) { val c = px[it]; (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000 }
        val threshold = gray.average() * 0.75
        val rowInk = IntArray(h)
        for (y in 0 until h) {
            var n = 0
            for (x in 0 until w) if (gray[y * w + x] < threshold) n++
            rowInk[y] = n
        }
        val minInk = maxOf(2, w / 200)
        val bands = mutableListOf<IntRange>()
        var start = -1
        for (y in 0 until h) {
            val ink = rowInk[y] >= minInk
            if (ink && start < 0) start = y
            if ((!ink || y == h - 1) && start >= 0) {
                bands += start..y
                start = -1
            }
        }
        // Kleine onderbrekingen (i-puntjes, lussen) samenvoegen en te dunne banden weggooien.
        val merged = mutableListOf<IntRange>()
        for (b in bands) {
            val last = merged.lastOrNull()
            if (last != null && b.first - last.last < h / 80) merged[merged.lastIndex] = last.first..b.last else merged += b
        }
        return merged.filter { it.last - it.first > h / 60 }.mapNotNull { band ->
            var left = w
            var right = 0
            for (y in band) for (x in 0 until w) if (gray[y * w + x] < threshold) { left = minOf(left, x); right = maxOf(right, x) }
            if (right <= left) null else Rect(left, band.first, right + 1, band.last + 1)
        }
    }
}
