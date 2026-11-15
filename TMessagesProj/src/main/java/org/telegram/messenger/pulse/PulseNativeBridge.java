package org.telegram.messenger.pulse;

import android.util.Log;

/**
 * JNI Bridge connecting Java/Android to the high-performance native Rust engine (libaether_core.so).
 */
public class PulseNativeBridge {

    private static final String TAG = "PulseNativeBridge";
    private static boolean isNativeLoaded = false;

    static {
        try {
            System.loadLibrary("aether_core");
            isNativeLoaded = true;
            Log.i(TAG, "libaether_core.so (Rust) loaded successfully!");
        } catch (Throwable t) {
            isNativeLoaded = false;
            Log.w(TAG, "Native Rust library not yet loaded, using fallback: " + t.getMessage());
        }
    }

    public static boolean isAvailable() {
        return isNativeLoaded;
    }

    // Native Rust Functions
    public static native boolean nativeIsLivingHuman(
            boolean isPrivate, boolean isBot, boolean isScam, boolean isFake, boolean isChannel, boolean hasMention
    );

    public static native void nativeProcessMessage(
            long dialogId, long userId, String displayName, String messageText,
            long nowMs, boolean isVoiceOrVideo, boolean hasMention, int unreadCount
    );

    public static native void nativeChatAnswered(long dialogId);

    public static native String nativeGetSortedHumansJson(long nowMs);

    public static native float[] nativeSpringStep(
            float position, float velocity, float target, float dt, float response, float dampingRatio
    );
}
