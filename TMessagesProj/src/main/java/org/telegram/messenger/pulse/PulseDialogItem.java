package org.telegram.messenger.pulse;

/**
 * Represents an active human contact detected by the Pulse Engine.
 */
public class PulseDialogItem implements Comparable<PulseDialogItem> {

    public long dialogId;
    public long userId;
    public String displayName;
    public String lastMessageSnippet;
    public long lastMessageDateMs;
    public int unreadCount;
    public boolean hasVoiceOrVideo;
    public boolean hasDirectMention;

    // Computed Urgency Metrics
    public float urgencyScore; // U in [0.0 ... 1.0]
    public float affinityScore; // A in [0.0 ... 1.0]
    public float intentScore;   // I in [0.0 ... 1.0]
    public int burstCount;      // Messages received within short burst window
    public boolean isPulsing;   // Active hysteresis state for neon halo

    public PulseDialogItem(long dialogId, long userId, String displayName) {
        this.dialogId = dialogId;
        this.userId = userId;
        this.displayName = displayName != null ? displayName : "";
        this.lastMessageDateMs = System.currentTimeMillis();
    }

    @Override
    public int compareTo(PulseDialogItem o) {
        // Highest urgency score first
        return Float.compare(o.urgencyScore, this.urgencyScore);
    }
}
