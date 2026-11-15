package org.telegram.messenger.pulse;

import android.graphics.Color;

/**
 * AetherTG "Тихий свет" (Quiet Light) Design System Color Palette.
 * Converts OKLCH-inspired perceptual coordinates into crisp sRGB for Android Canvas & Theme.
 */
public final class AetherColors {

    private AetherColors() {}

    // --- Night Theme (Dark Palette) ---
    public static final int NIGHT_BG_DEEP       = 0xFF0E1116; // Deep midnight background
    public static final int NIGHT_SURFACE       = 0xFF161A21; // Card / surface background
    public static final int NIGHT_SURFACE_LIGHT = 0xFF212631; // Elevated surface / popup
    public static final int NIGHT_TEXT_PRIMARY  = 0xFFE8E6E1; // Warm off-white primary text
    public static final int NIGHT_TEXT_QUIET    = 0xFF8A8F98; // Subdued secondary text
    public static final int NIGHT_BORDER        = 0x1AFFFFFF; // 10% white subtle outline

    // --- Paper Theme (Light Palette) ---
    public static final int PAPER_BG_DEEP       = 0xFFF5F1EA; // Warm paper background
    public static final int PAPER_SURFACE       = 0xFFFBF8F3; // Card surface
    public static final int PAPER_TEXT_PRIMARY  = 0xFF1F1D1A; // Warm charcoal primary text
    public static final int PAPER_TEXT_QUIET    = 0xFF77736C; // Subdued paper text
    public static final int PAPER_BORDER        = 0x14000000; // 8% black subtle outline

    // --- Accent Tokens ---
    // Amber Urgency (Replaces jarring red badges with warm urgency ember)
    public static final int EMBER_URGENT_NIGHT  = 0xFFF2B880; // Glowing warm ember
    public static final int EMBER_URGENT_PAPER  = 0xFFC9772E; // Deep amber for light mode
    public static final int EMBER_GLOW_START    = 0xCCF2B880; // 80% opacity amber
    public static final int EMBER_GLOW_END      = 0x00F2B880; // Fade to transparent

    // Moon Accent (Actions, highlights, active states)
    public static final int MOON_ACCENT_NIGHT   = 0xFF9FB4FF; // Soft periwinkle moon
    public static final int MOON_ACCENT_PAPER   = 0xFF4D63C9; // Slate indigo for light mode

    // Sage Accent ("Delivered", "Inbox Zero / Calm", verified)
    public static final int SAGE_CALM_NIGHT     = 0xFF8FC9A9; // Calming herbal sage
    public static final int SAGE_CALM_PAPER     = 0xFF3E8A63; // Forest sage

    /**
     * Blend two ARGB colors linearly by ratio [0.0 ... 1.0].
     */
    public static int blend(int colorFrom, int colorTo, float ratio) {
        float inverse = 1.0f - ratio;
        float a = Color.alpha(colorFrom) * inverse + Color.alpha(colorTo) * ratio;
        float r = Color.red(colorFrom) * inverse + Color.red(colorTo) * ratio;
        float g = Color.green(colorFrom) * inverse + Color.green(colorTo) * ratio;
        float b = Color.blue(colorFrom) * inverse + Color.blue(colorTo) * ratio;
        return Color.argb((int) a, (int) r, (int) g, (int) b);
    }
}
