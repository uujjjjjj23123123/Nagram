package org.telegram.messenger.pulse;

import android.graphics.Color;

/**
 * AetherTG // STARK OS Design System.
 * Engineered inspired by Stark Industries J.A.R.V.I.S. holographic HUD & Arc Reactor dynamics:
 * - Holographic laser cyan (#00F0FF) as primary reactor energy
 * - Arc Reactor secondary depth wave (#4D9FFF)
 * - Deep carbon & brushed titanium alloy background (#050B14 / #0A101D / #101826)
 * - Stark Mark 85 armor gold / repulsor energy (#FFB700) for human priority signals
 * - Repulsor critical alert red (#FF3333) for emergency overrides
 * - Clean cyan HUD wireframe grid lines
 */
public final class AetherColors {

    private AetherColors() {}

    // --- Stark Titanium Carbon Base ---
    public static final int STARK_VOID_BLACK    = 0xFF050B14; // Deep void space
    public static final int STARK_TITANIUM_DARK = 0xFF0A101D; // Armor plates base
    public static final int STARK_PANEL_SURFACE = 0xFF101826; // Holographic glass HUD panel
    public static final int STARK_PANEL_ELEVATED= 0xFF162235; // Elevated interactive module
    public static final int STARK_BORDER_CYAN   = 0x4000F0FF; // 25% holographic cyan wireframe border
    public static final int STARK_GRID_LINE     = 0x1A00F0FF; // 10% subtle HUD telemetry grid

    // --- Arc Reactor Core (Cyan / Teal) ---
    public static final int ARC_CYAN_CORE       = 0xFF00F0FF; // Pure laser cyan core
    public static final int ARC_CYAN_BRIGHT     = 0xFF85FAFF; // Hyper-bright focal point
    public static final int ARC_CYAN_GLOW       = 0x9900F0FF; // 60% ambient bloom
    public static final int ARC_CYAN_FAINT      = 0x1A00F0FF; // 10% holographic field
    public static final int ARC_BLUE_DEPTH      = 0xFF4D9FFF; // Secondary depth harmonic

    // --- Mark LXXXV Repulsor Energy (Amber / Gold / Crimson) ---
    public static final int REPULSOR_GOLD_CORE  = 0xFFFFB700; // Glowing repulsor energy
    public static final int REPULSOR_GOLD_GLOW  = 0xAAFFB700; // Overcharge corona bloom
    public static final int REPULSOR_CRIMSON    = 0xFFFF3333; // Tactical alert / emergency
    public static final int REPULSOR_EMBER      = 0xFFFFA020; // High urgency warm ember

    // --- Holographic HUD Typography ---
    public static final int HUD_TEXT_PRIMARY    = 0xFFE0F7FF; // Crisp holographic cyan-white
    public static final int HUD_TEXT_SECONDARY  = 0xFF6C8FA8; // Telemetry diagnostic cyan-gray
    public static final int HUD_TEXT_DIM        = 0xFF3D566E; // Background stream micro-data

    // --- Stark OS Chat Telemetry & Message Bubbles ---
    public static final int CHAT_WALLPAPER_BG   = 0xFF050B14; // Stark void space background
    public static final int CHAT_IN_BUBBLE      = 0xFF0D1829; // Deep armor titanium pod
    public static final int CHAT_IN_BUBBLE_SEL  = 0xFF13233B;
    public static final int CHAT_IN_TEXT        = 0xFFE2F4FF; // Holographic white-cyan
    public static final int CHAT_IN_TIME        = 0xFF537B9D; // Tactical telemetry slate
    public static final int CHAT_IN_REPLY_LINE  = 0xFF00F0FF; // Laser cyan accent

    public static final int CHAT_OUT_BUBBLE     = 0xFF0A2B4C; // Stark armor blue plate
    public static final int CHAT_OUT_GRADIENT_1 = 0xFF0D3761; // Apex energy
    public static final int CHAT_OUT_GRADIENT_2 = 0xFF0A2B4C; // Mid plate
    public static final int CHAT_OUT_GRADIENT_3 = 0xFF061A30; // Void shadow
    public static final int CHAT_OUT_BUBBLE_SEL = 0xFF103E6C;
    public static final int CHAT_OUT_TEXT       = 0xFFF2FBFF; // Diamond Stark white
    public static final int CHAT_OUT_TIME       = 0xFF4DA6FF; // Arc depth blue
    public static final int CHAT_OUT_CHECK_READ = 0xFF00F0FF; // Glowing Arc Cyan double check

    public static final int CHAT_PANEL_BG       = 0xFF070D18; // Holographic visor console dock
    public static final int CHAT_PANEL_TEXT     = 0xFFE2F4FF;
    public static final int CHAT_PANEL_HINT     = 0xFF4E7394;
    public static final int CHAT_ACTION_BAR     = 0xFF070D18; // Top HUD visor panel

    // --- Legacy Compatibility Aliases ---
    public static final int NIGHT_BG_DEEP       = STARK_VOID_BLACK;
    public static final int NIGHT_SURFACE       = STARK_TITANIUM_DARK;
    public static final int NIGHT_SURFACE_LIGHT = STARK_PANEL_SURFACE;
    public static final int NIGHT_TEXT_PRIMARY  = HUD_TEXT_PRIMARY;
    public static final int NIGHT_TEXT_QUIET    = HUD_TEXT_SECONDARY;
    public static final int NIGHT_BORDER        = STARK_BORDER_CYAN;

    public static final int PAPER_BG_DEEP       = 0xFF0D1524;
    public static final int PAPER_SURFACE       = 0xFF142036;
    public static final int PAPER_TEXT_PRIMARY  = 0xFFE0F7FF;
    public static final int PAPER_TEXT_QUIET    = 0xFF7A9BB8;
    public static final int PAPER_BORDER        = STARK_BORDER_CYAN;

    public static final int EMBER_URGENT_NIGHT  = REPULSOR_GOLD_CORE;
    public static final int EMBER_URGENT_PAPER  = REPULSOR_EMBER;
    public static final int EMBER_GLOW_START    = REPULSOR_GOLD_GLOW;
    public static final int EMBER_GLOW_END      = 0x00FFB700;

    public static final int MOON_ACCENT_NIGHT   = ARC_CYAN_CORE;
    public static final int MOON_ACCENT_PAPER   = ARC_BLUE_DEPTH;
    public static final int SAGE_CALM_NIGHT     = 0xFF00FF99; // Nominal systems green
    public static final int SAGE_CALM_PAPER     = 0xFF00CC7A;

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
