package org.telegram.messenger.pulse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AetherTG Pulse Engine (Детектор Живых Людей).
 * Implements Level 0/1/2 classification and the normalized Urgency Score (U) algorithm.
 */
public class PulseEngine {

    private static volatile PulseEngine instance;

    public interface PulseUpdateListener {
        void onPulseUpdated(List<PulseDialogItem> urgentHumans);
    }

    // Weights: w_A + w_I + w_B = 1.0
    private static final float WEIGHT_AFFINITY = 0.45f;
    private static final float WEIGHT_INTENT   = 0.35f;
    private static final float WEIGHT_BURST    = 0.20f;

    // Half-life T_1/2 = 30 minutes (1800 seconds) -> lambda = ln(2) / 1800
    private static final double LAMBDA = 0.69314718056 / 1800.0;

    // Hysteresis thresholds for neon halo
    private static final float THRESHOLD_GLOW_ON  = 0.80f;
    private static final float THRESHOLD_GLOW_OFF = 0.70f;

    // Urgency heuristic triggers
    private static final Pattern URGENT_WORDS = Pattern.compile(
            "(?iu)\\b(срочно|глянь|ты где|помоги|важно|набери|звоню|жду|быстро|горит|alert|urgent|asap|call me)\\b"
    );

    private final Map<Long, PulseDialogItem> activeHumans = new ConcurrentHashMap<>();
    private final List<PulseUpdateListener> listeners = new ArrayList<>();
    private SharedPreferences affinityPrefs;

    public static PulseEngine getInstance() {
        if (instance == null) {
            synchronized (PulseEngine.class) {
                if (instance == null) {
                    instance = new PulseEngine();
                }
            }
        }
        return instance;
    }

    private PulseEngine() {}

    public void init(Context context) {
        if (context != null) {
            affinityPrefs = context.getSharedPreferences("aether_affinity_store", Context.MODE_PRIVATE);
        }
    }

    public void addListener(PulseUpdateListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    public void removeListener(PulseUpdateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Level 0 / 1 / 2 Classifier:
     * Evaluates if a chat event belongs to a real living human.
     */
    public boolean isLivingHuman(boolean isPrivate, boolean isBot, boolean isScam, boolean isFake, boolean isChannel, boolean hasDirectMention) {
        // Broadcast channels and automated bots belong to Level 2 (Digest)
        if (isChannel || isBot || isScam || isFake) {
            return false;
        }
        // Direct human in private chat or explicit mention/reply in group
        return isPrivate || hasDirectMention;
    }

    /**
     * Process an incoming message event and recompute Urgency U.
     */
    public void onMessageReceived(long dialogId, long userId, String displayName, String messageText,
                                  boolean isVoiceOrVideo, boolean hasMention, int currentUnread) {
        long now = System.currentTimeMillis();

        PulseDialogItem item = activeHumans.get(dialogId);
        if (item == null) {
            item = new PulseDialogItem(dialogId, userId, displayName);
            activeHumans.put(dialogId, item);
        }

        item.displayName = displayName;
        item.lastMessageSnippet = messageText != null ? messageText : "";
        item.unreadCount = currentUnread;
        item.hasVoiceOrVideo = isVoiceOrVideo;
        item.hasDirectMention = hasMention;

        // 1. Calculate Burst count (messages within 60s)
        long deltaSec = Math.max(0, (now - item.lastMessageDateMs) / 1000);
        if (deltaSec <= 60) {
            item.burstCount++;
        } else {
            item.burstCount = 1;
        }
        item.lastMessageDateMs = now;

        // 2. Compute Intent Score I in [0.0 ... 1.0]
        float intent = 0.0f;
        if (messageText != null) {
            if (messageText.contains("?")) intent += 0.35f;
            if (URGENT_WORDS.matcher(messageText).find()) intent += 0.45f;
        }
        if (isVoiceOrVideo) intent += 0.25f;
        if (hasMention) intent += 0.30f;
        item.intentScore = Math.min(1.0f, intent);

        // 3. Affinity Score A in [0.0 ... 1.0] (Retrieved from cached response habits)
        item.affinityScore = getAffinity(userId);

        // 4. Normalized Burst B_hat = 1 - e^(-n / n0) where n0 = 2.0
        float bHat = (float) (1.0 - Math.exp(-item.burstCount / 2.0));

        // 5. Total Base Score: w_A*A + w_I*I + w_B*B_hat
        float baseScore = (WEIGHT_AFFINITY * item.affinityScore)
                        + (WEIGHT_INTENT * item.intentScore)
                        + (WEIGHT_BURST * bHat);

        // At arrival time t = 0, delta_t = 0 -> exp(-lambda * 0) = 1.0
        item.urgencyScore = Math.min(1.0f, Math.max(0.0f, baseScore));

        // Hysteresis state for visual neon pulse
        if (item.urgencyScore >= THRESHOLD_GLOW_ON) {
            item.isPulsing = true;
        } else if (item.urgencyScore < THRESHOLD_GLOW_OFF) {
            item.isPulsing = false;
        }

        notifyListeners();
    }

    /**
     * When user responds to the chat, urgency resets immediately to zero (inbox answered).
     */
    public void onChatAnswered(long dialogId, long userId) {
        activeHumans.remove(dialogId);
        incrementAffinityCounter(userId);
        notifyListeners();
    }

    /**
     * Returns the sorted list of living humans awaiting response, sorted by Urgency U.
     */
    public List<PulseDialogItem> getSortedHumans() {
        long now = System.currentTimeMillis();
        List<PulseDialogItem> list = new ArrayList<>();

        for (PulseDialogItem item : activeHumans.values()) {
            if (item.unreadCount <= 0) {
                continue;
            }
            // Dynamic exponential decay calculation at read time (zero background work)
            double elapsedSec = Math.max(0, (now - item.lastMessageDateMs) / 1000.0);
            double decay = Math.exp(-LAMBDA * elapsedSec);
            item.urgencyScore = (float) (item.urgencyScore * decay);

            // Update hysteresis on decay
            if (item.urgencyScore < THRESHOLD_GLOW_OFF) {
                item.isPulsing = false;
            }

            list.add(item);
        }

        Collections.sort(list);
        return list;
    }

    private float getAffinity(long userId) {
        if (affinityPrefs == null) return 0.5f;
        int answers = affinityPrefs.getInt("aff_" + userId, 3);
        // Normalize answer count into [0.2 ... 1.0]
        return Math.min(1.0f, 0.2f + (answers * 0.08f));
    }

    private void incrementAffinityCounter(long userId) {
        if (affinityPrefs != null) {
            int current = affinityPrefs.getInt("aff_" + userId, 3);
            affinityPrefs.edit().putInt("aff_" + userId, Math.min(20, current + 1)).apply();
        }
    }

    private void notifyListeners() {
        List<PulseDialogItem> snapshot = getSortedHumans();
        synchronized (listeners) {
            for (PulseUpdateListener l : listeners) {
                l.onPulseUpdated(snapshot);
            }
        }
    }
}
