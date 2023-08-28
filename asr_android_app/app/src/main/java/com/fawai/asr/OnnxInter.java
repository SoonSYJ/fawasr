package com.fawai.asr;

public class OnnxInter {
    static {
        System.loadLibrary("fawasr-jni");
    }

    public static native void ASRInitOnline(String modelDir);
    public static native String ASRInferOnline(byte[] waveform, boolean input_finished);
//    public static native void VADInitOnline(String modelDir);
//    public static native String VADInferOnline(float[] waveform, boolean input_finished);
}
