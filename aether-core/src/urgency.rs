use once_cell::sync::Lazy;
use regex::Regex;

pub const WEIGHT_AFFINITY: f32 = 0.45;
pub const WEIGHT_INTENT: f32 = 0.35;
pub const WEIGHT_BURST: f32 = 0.20;

// Half-life T_1/2 = 30 min (1800 sec) -> lambda = ln(2) / 1800
pub const LAMBDA: f64 = 0.6931471805599453 / 1800.0;

pub const THRESHOLD_GLOW_ON: f32 = 0.80;
pub const THRESHOLD_GLOW_OFF: f32 = 0.70;

static URGENT_REGEX: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"(?iu)\b(срочно|глянь|ты где|помоги|важно|набери|звоню|жду|быстро|горит|alert|urgent|asap|call me)\b")
        .expect("Valid regex")
});

/// Evaluates the intent score I in [0.0 ... 1.0] from message text and media type.
pub fn calculate_intent(text: &str, is_voice_or_video: bool, has_mention: bool) -> f32 {
    let mut score = 0.0f32;
    if text.contains('?') {
        score += 0.35;
    }
    if URGENT_REGEX.is_match(text) {
        score += 0.45;
    }
    if is_voice_or_video {
        score += 0.25;
    }
    if has_mention {
        score += 0.30;
    }
    score.clamp(0.0, 1.0)
}

/// Normalized burst factor: B_hat = 1 - exp(-n / n0) where n0 = 2.0
pub fn calculate_burst_hat(burst_count: u32) -> f32 {
    1.0 - (- (burst_count as f32) / 2.0).exp()
}

/// Calculates base urgency score at t = 0: U_base = w_A * A + w_I * I + w_B * B_hat
pub fn calculate_base_urgency(affinity: f32, intent: f32, burst_hat: f32) -> f32 {
    let score = (WEIGHT_AFFINITY * affinity.clamp(0.0, 1.0))
        + (WEIGHT_INTENT * intent.clamp(0.0, 1.0))
        + (WEIGHT_BURST * burst_hat.clamp(0.0, 1.0));
    score.clamp(0.0, 1.0)
}

/// Applies exponential time decay: U(t) = U_base * exp(-lambda * delta_t_sec)
pub fn apply_time_decay(base_urgency: f32, delta_t_sec: f64) -> f32 {
    if delta_t_sec <= 0.0 {
        return base_urgency;
    }
    let decay = (-LAMBDA * delta_t_sec).exp() as f32;
    (base_urgency * decay).clamp(0.0, 1.0)
}

/// Evaluates hysteresis glow state: activates at >= 0.80, deactivates at < 0.70.
pub fn update_glow_state(current_state: bool, urgency: f32) -> bool {
    if urgency >= THRESHOLD_GLOW_ON {
        true
    } else if urgency < THRESHOLD_GLOW_OFF {
        false
    } else {
        current_state
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_intent_keywords() {
        let intent = calculate_intent("Привет! Срочно посмотри это?", false, false);
        assert!(intent >= 0.80);
    }

    #[test]
    fn test_time_decay_half_life() {
        let base = 1.0;
        let decayed = apply_time_decay(base, 1800.0);
        assert!((decayed - 0.5).abs() < 0.01);
    }

    #[test]
    fn test_hysteresis() {
        assert!(update_glow_state(false, 0.82));
        assert!(update_glow_state(true, 0.75));
        assert!(!update_glow_state(true, 0.65));
    }
}
