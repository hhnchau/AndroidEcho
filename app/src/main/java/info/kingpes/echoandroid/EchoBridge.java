package info.kingpes.echoandroid;

public class EchoBridge {
    /*
     * Loading our lib
     */
    static {
        System.loadLibrary("echo");
    }

    /*
     * jni function declarations
     */
    static native void createSLEngine(int rate, int framesPerBuf,
                                      long delayInMs, float decay);

    static native void deleteSLEngine();

    static native boolean configureEcho(int delayInMs, float decay);

    static native boolean createSLBufferQueueAudioPlayer();

    static native void deleteSLBufferQueueAudioPlayer();

    static native boolean createAudioRecorder();

    static native void deleteAudioRecorder();

    static native void startPlay();

    static native void stopPlay();
}
