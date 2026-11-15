use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PulseItem {
    pub dialog_id: i64,
    pub user_id: i64,
    pub display_name: String,
    pub last_message_snippet: String,
    pub last_message_date_ms: i64,
    pub unread_count: i32,
    pub has_voice_or_video: bool,
    pub has_direct_mention: bool,

    pub urgency_score: f32,
    pub affinity_score: f32,
    pub intent_score: f32,
    pub burst_count: u32,
    pub is_pulsing: bool,
}

impl PulseItem {
    pub fn new(dialog_id: i64, user_id: i64, display_name: String) -> Self {
        Self {
            dialog_id,
            user_id,
            display_name,
            last_message_snippet: String::new(),
            last_message_date_ms: 0,
            unread_count: 0,
            has_voice_or_video: false,
            has_direct_mention: false,
            urgency_score: 0.0,
            affinity_score: 0.5,
            intent_score: 0.0,
            burst_count: 0,
            is_pulsing: false,
        }
    }
}
