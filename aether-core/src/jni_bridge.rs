use crate::classifier::is_living_human;
use crate::engine::PulseEngineState;
use crate::physics::{SpringConfig, SpringState};
use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jfloatArray, jint, jlong, jstring};
use jni::JNIEnv;
use once_cell::sync::Lazy;

static GLOBAL_ENGINE: Lazy<PulseEngineState> = Lazy::new(PulseEngineState::new);

#[no_mangle]
pub extern "system" fn Java_org_telegram_messenger_pulse_PulseNativeBridge_nativeIsLivingHuman(
    _env: JNIEnv,
    _class: JClass,
    is_private: jboolean,
    is_bot: jboolean,
    is_scam: jboolean,
    is_fake: jboolean,
    is_channel: jboolean,
    has_mention: jboolean,
) -> jboolean {
    if is_living_human(
        is_private != 0,
        is_bot != 0,
        is_scam != 0,
        is_fake != 0,
        is_channel != 0,
        has_mention != 0,
    ) {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_org_telegram_messenger_pulse_PulseNativeBridge_nativeProcessMessage(
    mut env: JNIEnv,
    _class: JClass,
    dialog_id: jlong,
    user_id: jlong,
    display_name: JString,
    message_text: JString,
    now_ms: jlong,
    is_voice_or_video: jboolean,
    has_mention: jboolean,
    unread_count: jint,
) {
    let name_str: String = env
        .get_string(&display_name)
        .map(|s| s.into())
        .unwrap_or_default();
    let text_str: String = env
        .get_string(&message_text)
        .map(|s| s.into())
        .unwrap_or_default();

    GLOBAL_ENGINE.on_message_received(
        dialog_id,
        user_id,
        name_str,
        text_str,
        now_ms,
        is_voice_or_video != 0,
        has_mention != 0,
        unread_count,
    );
}

#[no_mangle]
pub extern "system" fn Java_org_telegram_messenger_pulse_PulseNativeBridge_nativeChatAnswered(
    _env: JNIEnv,
    _class: JClass,
    dialog_id: jlong,
) {
    GLOBAL_ENGINE.on_chat_answered(dialog_id);
}

#[no_mangle]
pub extern "system" fn Java_org_telegram_messenger_pulse_PulseNativeBridge_nativeGetSortedHumansJson(
    env: JNIEnv,
    _class: JClass,
    now_ms: jlong,
) -> jstring {
    let list = GLOBAL_ENGINE.get_sorted_humans(now_ms);
    let json_str = serde_json::to_string(&list).unwrap_or_else(|_| "[]".to_string());
    match env.new_string(json_str) {
        Ok(js) => js.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_org_telegram_messenger_pulse_PulseNativeBridge_nativeSpringStep(
    env: JNIEnv,
    _class: JClass,
    position: jfloat,
    velocity: jfloat,
    target: jfloat,
    dt: jfloat,
    response: jfloat,
    damping_ratio: jfloat,
) -> jfloatArray {
    let mut state = SpringState {
        position,
        velocity,
        target,
    };
    let config = SpringConfig {
        response: if response > 0.0 { response } else { 0.25 },
        damping_ratio: if damping_ratio > 0.0 { damping_ratio } else { 0.85 },
    };

    state.step(dt, config);

    let output_data = [state.position, state.velocity];
    match env.new_float_array(2) {
        Ok(arr) => {
            let _ = env.set_float_array_region(&arr, 0, &output_data);
            arr.into_raw()
        }
        Err(_) => std::ptr::null_mut(),
    }
}
