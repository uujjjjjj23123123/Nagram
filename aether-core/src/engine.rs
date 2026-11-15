use crate::models::PulseItem;
use crate::urgency::*;
use parking_lot::RwLock;
use std::collections::HashMap;

pub struct PulseEngineState {
    pub active_humans: RwLock<HashMap<i64, PulseItem>>,
}

impl Default for PulseEngineState {
    fn default() -> Self {
        Self::new()
    }
}

impl PulseEngineState {
    pub fn new() -> Self {
        Self {
            active_humans: RwLock::new(HashMap::new()),
        }
    }

    pub fn on_message_received(
        &self,
        dialog_id: i64,
        user_id: i64,
        display_name: String,
        message_text: String,
        now_ms: i64,
        is_voice_or_video: bool,
        has_mention: bool,
        unread_count: i32,
    ) {
        let mut map = self.active_humans.write();
        let item = map
            .entry(dialog_id)
            .or_insert_with(|| PulseItem::new(dialog_id, user_id, display_name.clone()));

        item.display_name = display_name;
        item.last_message_snippet = message_text.clone();
        item.unread_count = unread_count;
        item.has_voice_or_video = is_voice_or_video;
        item.has_direct_mention = has_mention;

        // Burst tracking (within 60 seconds)
        let delta_sec = ((now_ms - item.last_message_date_ms).max(0)) / 1000;
        if delta_sec <= 60 {
            item.burst_count = item.burst_count.saturating_add(1);
        } else {
            item.burst_count = 1;
        }
        item.last_message_date_ms = now_ms;

        // Compute scores
        item.intent_score = calculate_intent(&message_text, is_voice_or_video, has_mention);
        let burst_hat = calculate_burst_hat(item.burst_count);
        item.urgency_score = calculate_base_urgency(item.affinity_score, item.intent_score, burst_hat);
        item.is_pulsing = update_glow_state(item.is_pulsing, item.urgency_score);
    }

    pub fn on_chat_answered(&self, dialog_id: i64) {
        let mut map = self.active_humans.write();
        map.remove(&dialog_id);
    }

    pub fn get_sorted_humans(&self, now_ms: i64) -> Vec<PulseItem> {
        let map = self.active_humans.read();
        let mut list: Vec<PulseItem> = Vec::with_capacity(map.len());

        for item in map.values() {
            if item.unread_count <= 0 {
                continue;
            }
            let mut clone = item.clone();
            let elapsed_sec = ((now_ms - clone.last_message_date_ms).max(0) as f64) / 1000.0;
            clone.urgency_score = apply_time_decay(clone.urgency_score, elapsed_sec);
            clone.is_pulsing = update_glow_state(clone.is_pulsing, clone.urgency_score);
            list.push(clone);
        }

        // Sort descending by urgency score
        list.sort_by(|a, b| b.urgency_score.partial_cmp(&a.urgency_score).unwrap_or(std::cmp::Ordering::Equal));
        list
    }
}
