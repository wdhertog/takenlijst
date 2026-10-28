package nl.wdhertog.takenlijst.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import nl.wdhertog.takenlijst.models.Model
import nl.wdhertog.takenlijst.models.ModelManager
import kotlin.math.abs

const val SAMPLE_RATE = 16_000

/** Dunne JNI-laag rond whisper.cpp (zie app/src/main/cpp/whisper_jni.cpp). */
object WhisperLib {
    init {
        System.loadLibrary("whisperjni")
    }

    /** Laadt een ggml-model; geeft 0 terug als dat mislukt. */
    external fun initContext(modelPath: String): Long

    external fun freeContext(ptr: Long)

    /** Transcribeert 16 kHz mono samples; geeft UTF-8 tekst terug, of null bij een fout. */
    external fun transcribe(ptr: Long, samples: FloatArray, language: String, prompt: String, threads: Int): ByteArray?
}

/** Neemt op via de microfoon tot [stop] of tot de maximale duur, en geeft de samples terug. */
class VoiceRecorder {
    /** Geluidsniveau 0..1 voor de animatie tijdens het opnemen. */
    val level = MutableStateFlow(0f)
    @Volatile private var recording = false

    fun stop() { recording = false }

    @SuppressLint("MissingPermission") // het scherm vraagt RECORD_AUDIO voordat het opneemt
    suspend fun record(maxSeconds: Int = 60): FloatArray = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, SAMPLE_RATE) * 2,
        )
        check(rec.state == AudioRecord.STATE_INITIALIZED) { "De microfoon is niet beschikbaar" }
        val out = ShortArray(maxSeconds * SAMPLE_RATE)
        var length = 0
        val buf = ShortArray(SAMPLE_RATE / 10)
        recording = true
        try {
            rec.startRecording()
            while (recording && currentCoroutineContext().isActive && length < out.size) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var peak = 0
                for (i in 0 until minOf(n, out.size - length)) {
                    out[length++] = buf[i]
                    peak = maxOf(peak, abs(buf[i].toInt()))
                }
                level.value = (peak / 12000f).coerceIn(0f, 1f)
            }
        } finally {
            recording = false
            level.value = 0f
            rec.stop()
            rec.release()
        }
        FloatArray(length) { out[it] / 32768f }
    }
}

/** Zet ingesproken boodschappen om in tekst met whisper.cpp, volledig op het toestel. */
class SpeechEngine(private val context: Context) {
    private val lock = Mutex()

    suspend fun transcribe(samples: FloatArray): String = withContext(Dispatchers.Default) {
        lock.withLock {
            check(ModelManager.isDownloaded(context, Model.SPEECH)) { "Download eerst het spraakmodel" }
            val path = ModelManager.file(context, Model.SPEECH.files.first().first).absolutePath
            val ctx = WhisperLib.initContext(path)
            check(ctx != 0L) { "Het spraakmodel kon niet geladen worden" }
            try {
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
                val bytes = WhisperLib.transcribe(ctx, samples, "nl", PROMPT, threads) ?: error("Uitschrijven is mislukt")
                bytes.toString(Charsets.UTF_8).trim()
            } finally {
                WhisperLib.freeContext(ctx)
            }
        }
    }

    companion object {
        private const val PROMPT = "Boodschappen: melk, 2 pakken yoghurt, eieren, hagelslag, appels, 10 kilo hondenbrokken."
    }
}
