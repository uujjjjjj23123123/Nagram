package org.telegram.messenger.pulse;

import java.util.Calendar;

/**
 * Manages the Circadian (Суточный) ambient cycle:
 * Modulates the UI color temperature based on local sun position without battery drain.
 */
public final class CircadianManager {

    public enum Phase {
        DAWN,   // 06:00 - 10:00 (Warm peach highlight)
        DAY,    // 10:00 - 17:00 (Neutral crisp)
        SUNSET, // 17:00 - 21:00 (Amber twilight warmth)
        NIGHT   // 21:00 - 06:00 (Deep indigo, -15% contrast for eye relief)
    }

    private static volatile CircadianManager instance;

    public static CircadianManager getInstance() {
        if (instance == null) {
            synchronized (CircadianManager.class) {
                if (instance == null) {
                    instance = new CircadianManager();
                }
            }
        }
        return instance;
    }

    private CircadianManager() {}

    public Phase getCurrentPhase() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 10) {
            return Phase.DAWN;
        } else if (hour >= 10 && hour < 17) {
            return Phase.DAY;
        } else if (hour >= 17 && hour < 21) {
            return Phase.SUNSET;
        } else {
            return Phase.NIGHT;
        }
    }

    /**
     * Returns subtle ambient tint factor for background blending.
     */
    public int getAmbientTint(boolean isDark) {
        Phase phase = getCurrentPhase();
        if (isDark) {
            switch (phase) {
                case DAWN:   return 0x08FFB088; // 3% warm peach
                case SUNSET: return 0x0CF2A050; // 5% warm amber
                case NIGHT:  return 0x0A0F1A30; // 4% midnight blue
                case DAY:
                default:     return 0x00000000;
            }
        } else {
            switch (phase) {
                case DAWN:   return 0x08FFF5EA;
                case SUNSET: return 0x0AFCEAD6;
                case NIGHT:  return 0x0AECE8E1;
                case DAY:
                default:     return 0x00000000;
            }
        }
    }
}
