package org.telegram.ui.Components.pulse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.pulse.AetherColors;
import org.telegram.messenger.pulse.PulseDialogItem;
import org.telegram.messenger.pulse.PulseEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * AetherTG "Радар Живых Людей" (Human Radar) View.
 * Renders prioritized human contacts with harmonic spring physics and glowing amber urgency halos.
 */
public class HumanRadarView extends View implements PulseEngine.PulseUpdateListener {

    public interface OnHumanSelectedListener {
        void onHumanSelected(long dialogId);
    }

    private final List<PulseDialogItem> items = new ArrayList<>();
    private OnHumanSelectedListener selectedListener;

    private Paint avatarBgPaint;
    private Paint avatarTextPaint;
    private Paint glowPaint;
    private Paint namePaint;
    private Paint scoreBadgePaint;
    private Paint scoreTextPaint;

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
        avatarBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarBgPaint.setColor(AetherColors.NIGHT_SURFACE_LIGHT);

        avatarTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarTextPaint.setColor(AetherColors.NIGHT_TEXT_PRIMARY);
        avatarTextPaint.setTextAlign(Paint.Align.CENTER);
        avatarTextPaint.setTextSize(dp(16));
        avatarTextPaint.setFakeBoldText(true);

        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(AetherColors.NIGHT_TEXT_PRIMARY);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setTextSize(dp(11));

        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        scoreBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scoreBadgePaint.setColor(AetherColors.EMBER_URGENT_NIGHT);

        scoreTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scoreTextPaint.setColor(0xFF0E1116);
        scoreTextPaint.setTextAlign(Paint.Align.CENTER);
        scoreTextPaint.setTextSize(dp(9));
        scoreTextPaint.setFakeBoldText(true);

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

        // Ambient breathing pulse for high-urgency neon rings
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(2400);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.addUpdateListener(anim -> {
            pulsePhase = (float) anim.getAnimatedValue();
            if (hasActivePulsingItems()) {
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

    private boolean hasActivePulsingItems() {
        for (PulseDialogItem item : items) {
            if (item.isPulsing) return true;
        }
        return false;
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
        // Compact 92dp height for horizontal radar bar
        int height = (int) dp(96);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) return;

        updateSpringPhysics();

        float itemWidth = dp(76);
        float centerY = dp(42);
        float baseRadius = dp(26);

        float currentX = dp(16) - scrollX;

        for (int i = 0; i < items.size(); i++) {
            PulseDialogItem item = items.get(i);
            float itemCenterX = currentX + (itemWidth / 2f);

            // Dock Magnification effect on touch
            float scale = 1.0f;
            if (touchX >= 0) {
                float dist = Math.abs(touchX - itemCenterX);
                float sigma = dp(60);
                scale += 0.22f * (float) Math.exp(-(dist * dist) / (2 * sigma * sigma));
            }

            float r = baseRadius * scale;

            // 1. Draw Amber Halo if Urgency is high
            if (item.urgencyScore > 0.4f) {
                float haloAlpha = item.isPulsing ? (0.6f + 0.4f * pulsePhase) : 0.45f;
                int haloColor = AetherColors.blend(0x00F2B880, AetherColors.EMBER_URGENT_NIGHT, haloAlpha);
                glowPaint.setShader(new RadialGradient(itemCenterX, centerY, r + dp(12),
                        haloColor, 0x00F2B880, Shader.TileMode.CLAMP));
                canvas.drawCircle(itemCenterX, centerY, r + dp(12), glowPaint);
            }

            // 2. Draw Avatar circle
            canvas.drawCircle(itemCenterX, centerY, r, avatarBgPaint);

            // 3. Draw Initials
            String initial = item.displayName.isEmpty() ? "?" : item.displayName.substring(0, 1).toUpperCase();
            canvas.drawText(initial, itemCenterX, centerY + dp(6), avatarTextPaint);

            // 4. Draw Urgency Percentage Badge if > 0.5
            if (item.urgencyScore >= 0.5f) {
                float badgeX = itemCenterX + r * 0.7f;
                float badgeY = centerY - r * 0.7f;
                RectF badgeRect = new RectF(badgeX - dp(11), badgeY - dp(7), badgeX + dp(11), badgeY + dp(7));
                canvas.drawRoundRect(badgeRect, dp(6), dp(6), scoreBadgePaint);
                int pct = Math.round(item.urgencyScore * 100);
                canvas.drawText(pct + "%", badgeX, badgeY + dp(3.5f), scoreTextPaint);
            }

            // 5. Draw Name Label
            String truncatedName = item.displayName.length() > 9
                    ? item.displayName.substring(0, 8) + "…"
                    : item.displayName;
            canvas.drawText(truncatedName, itemCenterX, dp(82), namePaint);

            currentX += itemWidth;
        }

        // Keep animating if spring physics has momentum
        if (Math.abs(scrollX - targetScrollX) > 0.5f || Math.abs(velocityX) > 0.5f) {
            postInvalidateOnAnimation();
        }
    }

    private void updateSpringPhysics() {
        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) lastFrameTime = now;
        float dt = Math.min(0.033f, (now - lastFrameTime) / 1000f);
        lastFrameTime = now;

        if (!isDragging) {
            float displacement = scrollX - targetScrollX;
            float force = -SPRING_K * displacement - SPRING_DAMPING * velocityX;
            velocityX += force * dt;
            scrollX += velocityX * dt;
        } else {
            scrollX += (targetScrollX - scrollX) * 0.25f;
        }
    }

    private void clampTargetScroll() {
        float maxScroll = Math.max(0, (items.size() * dp(76)) - getWidth() + dp(32));
        if (targetScrollX < 0) targetScrollX = 0;
        if (targetScrollX > maxScroll) targetScrollX = maxScroll;
    }

    private void checkItemClick(float tapX) {
        float currentX = dp(16) - scrollX;
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
