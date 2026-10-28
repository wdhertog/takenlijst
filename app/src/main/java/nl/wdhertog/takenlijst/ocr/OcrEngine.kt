package nl.wdhertog.takenlijst.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Een herkende regel met hoe zeker de herkenning ervan is (0–100). */
data class OcrLine(val text: String, val confidence: Float)

/**
 * Tekstherkenning op het toestel. Gedrukte tekst gaat via Tesseract (Nederlands taalmodel zit in de
 * app); handschrift via TrOCR, waarbij Tesseract alleen de regels op de foto zoekt.
 */
class OcrEngine(private val context: Context) {
    private val lock = Mutex()
    private var trOcr: TrOcr? = null

    private fun dataDir(): File {
        val dir = File(context.filesDir, "tesseract")
        val target = File(dir, "tessdata/$LANGUAGE.traineddata")
        if (!target.exists()) {
            target.parentFile!!.mkdirs()
            val tmp = File(target.path + ".tmp")
            context.assets.open("tessdata/$LANGUAGE.traineddata").use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            }
            tmp.renameTo(target)
        }
        return dir
    }

    private inline fun <T> withTesseract(bitmap: Bitmap, block: (TessBaseAPI) -> T): T {
        val api = TessBaseAPI()
        try {
            check(api.init(dataDir().absolutePath, LANGUAGE)) { "Tesseract kon niet starten" }
            // Een boodschappenlijstje is één kolom tekst met regels van wisselende lengte.
            api.pageSegMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN
            api.setImage(bitmap)
            return block(api)
        } finally {
            api.recycle()
        }
    }

    suspend fun recognize(uri: Uri, handwriting: Boolean): List<OcrLine> = withContext(Dispatchers.Default) {
        lock.withLock {
            val bitmap = loadBitmap(uri) ?: error("Kan de foto niet openen")
            try {
                if (handwriting) readHandwriting(bitmap) else readPrinted(bitmap)
            } finally {
                bitmap.recycle()
            }
        }
    }

    private fun readPrinted(bitmap: Bitmap): List<OcrLine> = withTesseract(bitmap) { api ->
        api.getUTF8Text()
        val lines = mutableListOf<OcrLine>()
        val it = api.resultIterator
        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
        it.begin()
        do {
            val text = it.getUTF8Text(level)?.trim().orEmpty()
            if (text.isNotEmpty()) lines += OcrLine(text, it.confidence(level))
        } while (it.next(level))
        it.delete()
        lines
    }

    private fun readHandwriting(bitmap: Bitmap): List<OcrLine> {
        val engine = trOcr ?: TrOcr(context).also { trOcr = it }
        val boxes = lineBoxes(bitmap).ifEmpty { LineFinder.find(bitmap) }
        return boxes.sortedBy { it.top }.mapNotNull { box ->
            val pad = box.height() / 5
            val r = Rect(
                (box.left - pad).coerceAtLeast(0), (box.top - pad).coerceAtLeast(0),
                (box.right + pad).coerceAtMost(bitmap.width), (box.bottom + pad).coerceAtMost(bitmap.height),
            )
            if (r.width() < 8 || r.height() < 8) return@mapNotNull null
            val crop = Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height())
            try {
                engine.read(crop).let { it.copy(text = cleanHallucination(it.text)) }.takeIf { it.text.isNotBlank() }
            } finally {
                crop.recycle()
            }
        }
    }

    /** Regels volgens de lay-outanalyse van Tesseract; dat werkt ook bij handschrift redelijk. */
    private fun lineBoxes(bitmap: Bitmap): List<Rect> = withTesseract(bitmap) { api ->
        api.getUTF8Text()
        val boxes = mutableListOf<Rect>()
        val it = api.resultIterator
        val level = TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE
        it.begin()
        do {
            it.getBoundingRect(level)?.takeIf { r -> r.width() > 0 && r.height() > 0 }?.let(boxes::add)
        } while (it.next(level))
        it.delete()
        // Heel lage "regels" zijn meestal vlekjes of de lijntjes van het papier.
        val typical = boxes.map { it.height() }.sorted().getOrNull(boxes.size / 2) ?: 0
        boxes.filter { it.height() >= typical / 3 }
    }

    /** Foto inlezen, verkleinen tot een werkbare maat en rechtzetten volgens de EXIF-oriëntatie. */
    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val rotation = resolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) return decoded
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        if (rotated !== decoded) decoded.recycle()
        return rotated
    }

    companion object {
        private const val LANGUAGE = "nld"
        private const val MAX_SIDE = 1600

        /** TrOCR plakt soms " ." of een verzonnen vervolg achter een regel; alles vanaf " ." weghalen. */
        fun cleanHallucination(text: String): String = text.substringBefore(" .").trim()
    }
}
