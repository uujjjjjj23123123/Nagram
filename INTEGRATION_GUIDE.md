# AetherTG Android Integration Guide

Этот документ описывает, как модули **Pulse Engine**, **HumanRadarView** и **AetherColors** подключаются к ядру Telegram Android (`TMessagesProj`).

---

## 1. Инициализация PulseEngine

В главном приложении (класс `ApplicationLoader.java`):

```java
// В методе ApplicationLoader.onCreate():
org.telegram.messenger.pulse.PulseEngine.getInstance().init(applicationContext);
```

---

## 2. Перехват входящих сообщений (MessagesController.java)

В методе `processUpdates(TLRPC.Updates updates, ...)` или `checkUnreadDialogs()`:

```java
// При обработке входящего сообщения TLRPC.TL_updateNewMessage:
TLRPC.Message message = update.message;
long dialogId = message.dialog_id;
long fromId = MessageObject.getFromChatId(message);

TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
boolean isPrivate = DialogObject.isUserDialog(dialogId);
boolean isBot = user != null && user.bot;
boolean isScam = user != null && user.scam;
boolean isFake = user != null && user.fake;
boolean isChannel = ChatObject.isChannel(MessagesController.getInstance(currentAccount).getChat(-dialogId));
boolean hasMention = message.mentioned;

// Проверяем через классификатор Aether:
if (PulseEngine.getInstance().isLivingHuman(isPrivate, isBot, isScam, isFake, isChannel, hasMention)) {
    String name = UserObject.getUserName(user);
    String text = message.message;
    boolean isMedia = message.media instanceof TLRPC.TL_messageMediaVoice 
                   || message.media instanceof TLRPC.TL_messageMediaRoundVideo;
    int unreadCount = MessagesController.getInstance(currentAccount).getDialogUnreadCount(dialogId);

    PulseEngine.getInstance().onMessageReceived(
        dialogId, fromId, name, text, isMedia, hasMention, unreadCount
    );
}
```

Когда пользователь открывает диалог и отвечает (`markDialogAsRead` или `sendMessage`):
```java
PulseEngine.getInstance().onChatAnswered(dialogId, fromId);
```

---

## 3. Встраивание HumanRadarView в DialogsActivity.java

В `DialogsActivity.java` (экран всех чатов):

1. **Создание View в `createView(Context context)`:**
```java
HumanRadarView radarView = new HumanRadarView(context);
radarView.setOnHumanSelectedListener(dialogId -> {
    // Мгновенный переход в диалог по тапу на аватарку в радаре:
    Bundle args = new Bundle();
    args.putLong("user_id", dialogId);
    presentFragment(new ChatActivity(args));
});

// Добавляем radarView в шапку contentView над listView:
contentView.addView(radarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 96));
```

---

## 4. Палитра «Тихий свет» в Theme.java

В `Theme.java` переопределяем цвета дефолтной темной темы:
* `key_chat_inBubble` $\rightarrow$ `AetherColors.NIGHT_SURFACE` (`#161A21`)
* `key_windowBackgroundWhite` $\rightarrow$ `AetherColors.NIGHT_BG_DEEP` (`#0E1116`)
* `key_chats_unreadCounter` $\rightarrow$ `AetherColors.EMBER_URGENT_NIGHT` (`#F2B880`)
* `key_chats_actionBackground` $\rightarrow$ `AetherColors.MOON_ACCENT_NIGHT` (`#9FB4FF`)
