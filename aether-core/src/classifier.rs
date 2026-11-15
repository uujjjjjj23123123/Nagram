#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ChatPriorityLevel {
    /// Level 0: Direct personal human interaction or direct mention
    LivingHuman,
    /// Level 1: Active participating group
    TeamGroup,
    /// Level 2: Isolated channels, bots, and digests
    Digest,
}

pub fn classify_chat(
    is_private: bool,
    is_bot: bool,
    is_scam: bool,
    is_fake: bool,
    is_channel: bool,
    has_mention: bool,
) -> ChatPriorityLevel {
    if is_channel || is_bot || is_scam || is_fake {
        ChatPriorityLevel::Digest
    } else if is_private || has_mention {
        ChatPriorityLevel::LivingHuman
    } else {
        ChatPriorityLevel::TeamGroup
    }
}

pub fn is_living_human(
    is_private: bool,
    is_bot: bool,
    is_scam: bool,
    is_fake: bool,
    is_channel: bool,
    has_mention: bool,
) -> bool {
    classify_chat(is_private, is_bot, is_scam, is_fake, is_channel, has_mention)
        == ChatPriorityLevel::LivingHuman
}
