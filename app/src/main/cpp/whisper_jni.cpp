#include <jni.h>
#include <string>
#include <android/log.h>
#include "whisper.h"

#define LOG_TAG "WhisperJni"

extern "C" JNIEXPORT jlong JNICALL
Java_nl_wdhertog_takenlijst_voice_WhisperLib_initContext(JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);
    whisper_context_params params = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(path, params);
    if (ctx == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Model laden mislukt: %s", path);
    }
    env->ReleaseStringUTFChars(modelPath, path);
    return reinterpret_cast<jlong>(ctx);
}

extern "C" JNIEXPORT void JNICALL
Java_nl_wdhertog_takenlijst_voice_WhisperLib_freeContext(JNIEnv *, jobject, jlong ptr) {
    whisper_free(reinterpret_cast<whisper_context *>(ptr));
}

// Geeft UTF-8 bytes terug i.p.v. een jstring: whisper kan halve multibyte-tekens per segment
// opleveren, en NewStringUTF crasht op ongeldige UTF-8.
extern "C" JNIEXPORT jbyteArray JNICALL
Java_nl_wdhertog_takenlijst_voice_WhisperLib_transcribe(JNIEnv *env, jobject, jlong ptr,
                                                        jfloatArray samples, jstring language,
                                                        jstring prompt, jint threads) {
    auto *ctx = reinterpret_cast<whisper_context *>(ptr);
    const char *lang = env->GetStringUTFChars(language, nullptr);
    const char *initialPrompt = env->GetStringUTFChars(prompt, nullptr);
    jsize count = env->GetArrayLength(samples);
    jfloat *data = env->GetFloatArrayElements(samples, nullptr);

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.no_context = true;
    params.single_segment = false;
    params.language = lang;
    params.n_threads = threads;
    // Een voorbeeldlijstje zet whisper op het spoor van komma's en productnamen.
    params.initial_prompt = initialPrompt;

    int rc = whisper_full(ctx, params, data, count);

    env->ReleaseFloatArrayElements(samples, data, JNI_ABORT);
    env->ReleaseStringUTFChars(language, lang);
    env->ReleaseStringUTFChars(prompt, initialPrompt);

    if (rc != 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "whisper_full faalde: %d", rc);
        return nullptr;
    }

    std::string text;
    int segments = whisper_full_n_segments(ctx);
    for (int i = 0; i < segments; i++) {
        text += whisper_full_get_segment_text(ctx, i);
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(text.size()));
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(text.size()),
                            reinterpret_cast<const jbyte *>(text.data()));
    return result;
}
