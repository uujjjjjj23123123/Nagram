package org.telegram.ui.Components.pulse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.pulse.AetherColors;
import org.telegram.messenger.pulse.PulseDialogItem;
import org.telegram.messenger.pulse.PulseEngine;
import org.telegram.messenger.pulse.PulseNativeBridge;

import java.util.ArrayList;
import java.util.List;

/**
 * AetherTG // STARK OS "Тактический Радар Живых Людей" (Arc Reactor Tactical Radar).
 * Designed in the Stark Industries / J.A.R.V.I.S. HUD visual system:
 * - Rotating Arc Reactor stator coils around living human nodes
 * - Holographic laser cyan (#00F0FF) telemetry & targeting reticles
 * - Overcharge Repulsor Gold (#FFB700) bloom for high-urgency contacts
 * - Glassmorphic titanium panel with HUD corner brackets
 * - Harmonic spring physics powered by native Rust core (libaether_core.so)
 */
public class HumanRadarView extends View implements PulseEngine.PulseUpdateListener {

    public interface OnHumanSelectedListener {
        void onHumanSelected(long dialogId);
    }

    private final List<PulseDialogItem> items = new ArrayList<>();
    private OnHumanSelectedListener selectedListener;

    // HUD & Arc Reactor Paints
    private Paint hudPanelPaint;
    private Paint hudBorderPaint;
    private Paint hudCornerPaint;
    private Paint hudTelemetryPaint;
    private Paint ledPaint;

    private Paint avatarBgPaint;
    private Paint avatarTextPaint;
    private Paint glowPaint;
    private Paint namePaint;
    private Paint scoreBadgePaint;
    private Paint scoreTextPaint;

    private Paint arcStatorPaint;
    private Paint arcCoreRingPaint;
    private Paint reticlePaint;

    private float scrollX = 0f;
    private float targetScrollX = 0f;
    private float velocityX = 0f;
    private boolean isDragging = false;
    private float touchX = -1f;

    // Harmonic Spring constants: Response T = 0.25s, Damping ratio = 0.85
    private static final float SPRING_K = 280f; // Stiffness
    private static final float SPRING_DAMPING = 22f; // Friction
    private long lastFrameTime = 0;

    private float pulsePhase = 0f;
    private ValueAnimator pulseAnimator;

    private GestureDetector gestureDetector;

    public HumanRadarView(Context context) {
        super(context);
        init();
    }

    public HumanRadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setVisibility(View.GONE);

        // 1. HUD Panel Container
        hudPanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudPanelPaint.setColor(0xED070C16); // 93% opaque dark titanium void

        hudBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudBorderPaint.setStyle(Paint.Style.STROKE);
        hudBorderPaint.setColor(AetherColors.STARK_BORDER_CYAN);
        hudBorderPaint.setStrokeWidth(dp(1));

        hudCornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudCornerPaint.setStyle(Paint.Style.STROKE);
        hudCornerPaint.setColor(AetherColors.ARC_CYAN_CORE);
        hudCornerPaint.setStrokeWidth(dp(1.75f));

        hudTelemetryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hudTelemetryPaint.setColor(AetherColors.ARC_CYAN_CORE);
        hudTelemetryPaint.setTextSize(dp(8.5f));
        hudTelemetryPaint.setTypeface(Typeface.MONOSPACE);
        hudTelemetryPaint.setLetterSpacing(0.08f);

        ledPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ledPaint.setColor(AetherColors.ARC_CYAN_CORE);

        // 2. Arc Reactor Nodes
        avatarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarBgPaint.setColor(AetherColors.STARK_PANEL_ELEVATED);

        avatarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarTextPaint.setColor(AetherColors.HUD_TEXT_PRIMARY);
        avatarTextPaint.setTextAlign(Paint.Align.CENTER);
        avatarTextPaint.setTextSize(dp(15));
        avatarTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        arcStatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcStatorPaint.setStyle(Paint.Style.STROKE);
        arcStatorPaint.setColor(AetherColors.ARC_CYAN_CORE);
        arcStatorPaint.setStrokeWidth(dp(1.5f));

        arcCoreRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcCoreRingPaint.setStyle(Paint.Style.STROKE);
        arcCoreRingPaint.setStrokeWidth(dp(2));

        reticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        reticlePaint.setStyle(Paint.Style.STROKE);
        reticlePaint.setColor(AetherColors.REPULSOR_GOLD_CORE);
        reticlePaint.setStrokeWidth(dp(1.2f));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(AetherColors.HUD_TEXT_PRIMARY);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTextSize(dp(10.5f));
        namePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));

        scoreBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scoreTextPaint.setColor(0xFF050B14);
        scoreTextPaint.setTextAlign(Paint.Align.CENTER);
        scoreTextPaint.setTextSize(dp(8.5f));
        scoreTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                isDragging = true;
                velocityX = 0f;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                targetScrollX += distanceX;
                clampTargetScroll();
                invalidate();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                velocityX = -vX * 0.015f;
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                checkItemClick(e.getX());
                return true;
            }
        });

        // Breathing pulse for energetic Arc halos
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(anim -> {
            pulsePhase = (float) anim.getAnimatedValue();
            if (getVisibility() == View.VISIBLE && !items.isEmpty()) {
                invalidate();
            }
        });
        pulseAnimator.start();

        PulseEngine.getInstance().addListener(this);
    }

    public void setOnHumanSelectedListener(OnHumanSelectedListener listener) {
        this.selectedListener = listener;
    }

    @Override
    public void onPulseUpdated(List<PulseDialogItem> urgentHumans) {
        post(() -> {
            items.clear();
            items.addAll(urgentHumans);
            setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
            clampTargetScroll();
            invalidate();
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchX = event.getX();
        boolean handled = gestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            isDragging = false;
            touchX = -1f;
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getVisibility() == View.GONE || items.isEmpty()) {
            setMeasuredDimension(0, 0);
            return;
        }
        // Tactical 108dp HUD bar height
        int height = (int) dp(108);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getVisibility() == View.GONE || items.isEmpty()) return;

        updateSpringPhysics();

        float width = getWidth();
        float height = getHeight();
        float padH = dp(8);
        float padV = dp(4);

        // 1. Draw Holographic HUD Panel Background
        RectF panelRect = new RectF(padH, padV, width - padH, height - padV);
        canvas.drawRoundRect(panelRect, dp(12), dp(12), hudPanelPaint);
        canvas.drawRoundRect(panelRect, dp(12), dp(12), hudBorderPaint);

        // 2. Draw HUD Technical Corner Brackets
        float bracketLen = dp(10);
        // Top-left
        canvas.drawLine(padH, padV + bracketLen, padH, padV, hudCornerPaint);
        canvas.drawLine(padH, padV, padH + bracketLen, padV, hudCornerPaint);
        // Top-right
        canvas.drawLine(width - padH - bracketLen, padV, width - padH, padV, hudCornerPaint);
        canvas.drawLine(width - padH, padV, width - padH, padV + bracketLen, hudCornerPaint);
        // Bottom-left
        canvas.drawLine(padH, height - padV - bracketLen, padH, height - padV, hudCornerPaint);
        canvas.drawLine(padH, height - padV, padH + bracketLen, height - padV, hudCornerPaint);
        // Bottom-right
        canvas.drawLine(width - padH - bracketLen, height - padV, width - padH, height - padV, hudCornerPaint);
        canvas.drawLine(width - padH, height - padV - bracketLen, width - padH, height - padV, hudCornerPaint);

        // 3. Draw Telemetry Header
        String telemetry = "◢ STARK TACTICAL RADAR // [ARC CORE: 100%] // VITAL SIGNS: " + items.size() + " ◣";
        canvas.drawText(telemetry, padH + dp(18), padV + dp(14), hudTelemetryPaint);

        // Pulsing Live Indicator Dot
        float ledAlpha = 0.5f + 0.5f * pulsePhase;
        ledPaint.setColor(AetherColors.blend(0x0000F0FF, AetherColors.ARC_CYAN_CORE, ledAlpha));
        canvas.drawCircle(padH + dp(10), padV + dp(11.5f), dp(3), ledPaint);

        // 4. Draw Living Contact Nodes (Arc Reactor Modules)
        float itemWidth = dp(76);
        float centerY = dp(54);
        float baseRadius = dp(24);
        float currentX = dp(20) - scrollX;

        long now = System.currentTimeMillis();
        float continuousRotation = (now % 6000) / 6000f * 360f;

        for (int i = 0; i < items.size(); i++) {
            PulseDialogItem item = items.get(i);
            float itemCenterX = currentX + (itemWidth / 2f);

            // Dock Magnification effect on touch
            float scale = 1.0f;
            if (touchX >= 0) {
                float dist = Math.abs(touchX - itemCenterX);
                float sigma = dp(60);
                scale += 0.24f * (float) Math.exp(-(dist * dist) / (2 * sigma * sigma));
            }

            float r = baseRadius * scale;
            boolean isHighUrgency = item.urgencyScore > 0.55f;
            int coreColor = isHighUrgency ? AetherColors.REPULSOR_GOLD_CORE : AetherColors.ARC_CYAN_CORE;

            // A. Draw Arc Reactor Energy Bloom Halo
            if (item.urgencyScore > 0.35f) {
                float haloAlpha = item.isPulsing ? (0.6f + 0.4f * pulsePhase) : (0.4f + 0.3f * pulsePhase);
                int glowColor = AetherColors.blend(0x00000000, coreColor, haloAlpha);
                glowPaint.setShader(new RadialGradient(itemCenterX, centerY, r + dp(16),
                        glowColor, 0x00000000, Shader.TileMode.CLAMP));
                canvas.drawCircle(itemCenterX, centerY, r + dp(16), glowPaint);
            }

            // B. Draw Rotating Arc Reactor Stator Ring (8 Coils)
            canvas.save();
            canvas.rotate(continuousRotation + (i * 45), itemCenterX, centerY);
            arcStatorPaint.setColor(coreColor);
            float statorRadius = r + dp(4.5f);
            for (int k = 0; k < 8; k++) {
                float angleRad = (float) Math.toRadians(k * 45);
                float x1 = itemCenterX + (float) Math.cos(angleRad) * statorRadius;
                float y1 = centerY + (float) Math.sin(angleRad) * statorRadius;
                float x2 = itemCenterX + (float) Math.cos(angleRad) * (statorRadius + dp(3.5f));
                float y2 = centerY + (float) Math.sin(angleRad) * (statorRadius + dp(3.5f));
                canvas.drawLine(x1, y1, x2, y2, arcStatorPaint);
            }
            canvas.restore();

            // C. Draw Target Reticle Brackets [ + ] if High Urgency
            if (isHighUrgency) {
                float reticleR = r + dp(9);
                reticlePaint.setColor(AetherColors.REPULSOR_GOLD_CORE);
                // Top-left bracket
                canvas.drawLine(itemCenterX - reticleR, centerY - reticleR + dp(5), itemCenterX - reticleR, centerY - reticleR, reticlePaint);
                canvas.drawLine(itemCenterX - reticleR, centerY - reticleR, itemCenterX - reticleR + dp(5), centerY - reticleR, reticlePaint);
                // Top-right bracket
                canvas.drawLine(itemCenterX + reticleR - dp(5), centerY - reticleR, itemCenterX + reticleR, centerY - reticleR, reticlePaint);
                canvas.drawLine(itemCenterX + reticleR, centerY - reticleR, itemCenterX + reticleR, centerY - reticleR + dp(5), reticlePaint);
                // Bottom-left bracket
                canvas.drawLine(itemCenterX - reticleR, centerY + reticleR - dp(5), itemCenterX - reticleR, centerY + reticleR, reticlePaint);
                canvas.drawLine(itemCenterX - reticleR, centerY + reticleR, itemCenterX - reticleR + dp(5), centerY + reticleR, reticlePaint);
                // Bottom-right bracket
                canvas.drawLine(itemCenterX + reticleR - dp(5), centerY + reticleR, itemCenterX + reticleR, centerY + reticleR, reticlePaint);
                canvas.drawLine(itemCenterX + reticleR, centerY + reticleR - dp(5), itemCenterX + reticleR, centerY + reticleR, reticlePaint);
            }

            // D. Draw Inner Reactor Core Ring & Avatar Body
            arcCoreRingPaint.setColor(coreColor);
            canvas.drawCircle(itemCenterX, centerY, r + dp(1.5f), arcCoreRingPaint);
            canvas.drawCircle(itemCenterX, centerY, r, avatarBgPaint);

            // E. Draw Contact Initial
            String initial = item.displayName.isEmpty() ? "?" : item.displayName.substring(0, 1).toUpperCase();
            canvas.drawText(initial, itemCenterX, centerY + dp(5.5f), avatarTextPaint);

            // F. Draw Arc Reactor Telemetry Pill Badge: "⚡ 85%"
            if (item.urgencyScore >= 0.4f) {
                float badgeX = itemCenterX + r * 0.72f;
                float badgeY = centerY - r * 0.72f;
                RectF badgeRect = new RectF(badgeX - dp(12), badgeY - dp(7), badgeX + dp(12), badgeY + dp(7));
                scoreBadgePaint.setColor(coreColor);
                canvas.drawRoundRect(badgeRect, dp(6), dp(6), scoreBadgePaint);
                int pct = Math.round(item.urgencyScore * 100);
                canvas.drawText(pct + "%", badgeX, badgeY + dp(3.2f), scoreTextPaint);
            }

            // G. Draw Contact Name
            String truncatedName = item.displayName.length() > 9
                    ? item.displayName.substring(0, 8) + "…"
                    : item.displayName;
            canvas.drawText(truncatedName, itemCenterX, dp(96), namePaint);

            currentX += itemWidth;
        }

        // Keep continuous animation running smoothly for rotating Arc Reactor rings
        postInvalidateOnAnimation();
    }

    private void updateSpringPhysics() {
        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = Math.min(0.033f, (now - lastFrameTime) / 1000f);
        lastFrameTime = now;

        if (!isDragging) {
            if (PulseNativeBridge.isAvailable()) {
                float[] step = PulseNativeBridge.nativeSpringStep(scrollX, velocityX, targetScrollX, dt, 0.25f, 0.85f);
                if (step != null && step.length >= 2) {
                    scrollX = step[0];
                    velocityX = step[1];
                    return;
                }
            }
            float displacement = scrollX - targetScrollX;
            float force = -SPRING_K * displacement - SPRING_DAMPING * velocityX;
            velocityX += force * dt;
            scrollX += velocityX * dt;
        } else {
            scrollX += (targetScrollX - scrollX) * 0.25f;
        }
    }

    private void clampTargetScroll() {
        float maxScroll = Math.max(0, (items.size() * dp(76)) - getWidth() + dp(40));
        if (targetScrollX < 0) targetScrollX = 0;
        if (targetScrollX > maxScroll) targetScrollX = maxScroll;
    }

    private void checkItemClick(float tapX) {
        float currentX = dp(20) - scrollX;
        float itemWidth = dp(76);
        for (PulseDialogItem item : items) {
            if (tapX >= currentX && tapX <= currentX + itemWidth) {
                if (selectedListener != null) {
                    selectedListener.onHumanSelected(item.dialogId);
                }
                break;
            }
            currentX += itemWidth;
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
        PulseEngine.getInstance().removeListener(this);
    }
}
