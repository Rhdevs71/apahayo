#include <jni.h>
#include <string>
#include <opusenc.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeInitOpusEncoder(JNIEnv *env, jclass clazz, jstring outputPath, jint sampleRate, jint channels) {
    const char *path = env->GetStringUTFChars(outputPath, 0);
    
    int error = 0;
    OggOpusComments *comments = ope_comments_create();
    ope_comments_add(comments, "ENCODER", "RHPatch");
    
    OggOpusEnc *enc = ope_encoder_create_file(path, comments, sampleRate, channels, 0, &error);
    
    env->ReleaseStringUTFChars(outputPath, path);
    ope_comments_destroy(comments);
    
    if (error != OPE_OK || !enc) {
        return 0;
    }
    
    // Force bitrate to 64000
    ope_encoder_ctl(enc, OPUS_SET_BITRATE(64000));
    
    return reinterpret_cast<jlong>(enc);
}

extern "C" JNIEXPORT void JNICALL
Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeEncodeOpus(JNIEnv *env, jclass clazz, jlong handle, jbyteArray pcm, jint length) {
    if (handle == 0 || pcm == nullptr) return;
    OggOpusEnc *enc = reinterpret_cast<OggOpusEnc *>(handle);
    
    jbyte *pcmData = env->GetByteArrayElements(pcm, NULL);
    
    // length is in bytes. 16-bit PCM = 2 bytes per sample.
    // frames = number of samples per channel.
    // Our Java code guarantees mono (channels = 1), so frames = length / 2
    int frames = length / 2;
    ope_encoder_write(enc, reinterpret_cast<const opus_int16 *>(pcmData), frames);
    
    env->ReleaseByteArrayElements(pcm, pcmData, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL
Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeCloseOpusEncoder(JNIEnv *env, jclass clazz, jlong handle) {
    if (handle == 0) return;
    OggOpusEnc *enc = reinterpret_cast<OggOpusEnc *>(handle);
    ope_encoder_drain(enc);
    ope_encoder_destroy(enc);
}

// Backward compatibility symbols
extern "C" JNIEXPORT jlong JNICALL
Java_com_wmods_wppenhacer_xposed_utils_AudioOpusConverter_nativeInitOpusEncoder(JNIEnv *env, jclass clazz, jstring outputPath, jint sampleRate, jint channels) {
    return Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeInitOpusEncoder(env, clazz, outputPath, sampleRate, channels);
}

extern "C" JNIEXPORT void JNICALL
Java_com_wmods_wppenhacer_xposed_utils_AudioOpusConverter_nativeEncodeOpus(JNIEnv *env, jclass clazz, jlong handle, jbyteArray pcm, jint length) {
    Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeEncodeOpus(env, clazz, handle, pcm, length);
}

extern "C" JNIEXPORT void JNICALL
Java_com_wmods_wppenhacer_xposed_utils_AudioOpusConverter_nativeCloseOpusEncoder(JNIEnv *env, jclass clazz, jlong handle) {
    Java_com_rhdevs_rhpatch_xposed_utils_AudioOpusConverter_nativeCloseOpusEncoder(env, clazz, handle);
}

