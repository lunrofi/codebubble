# KodBalonu

![KodBalonu icon](icon.png)

**KodBalonu** catches verification codes from SMS and app notifications,
copies them to the clipboard, and shows a draggable bubble. Long-press the
code in the bubble and drag it onto a text field to drop the code there.

- **Version:** 3.0 (version code 3)
- **Download:** [kodbalonu-3.0.apk](releases/kodbalonu-3.0.apk)
- **Package:** `com.bmo.kodbalonu`
- **Min SDK:** 26 (Android 8.0) · **Target SDK:** 34 (Android 14)
- **Size:** ~153 KB — no heavy libraries, plain Java, no Gradle needed
- **License:** MIT (see [LICENSE](LICENSE))

> KodBalonu is written from scratch. It does not contain code from any
> other code-catcher app. The sensitive-phrase list is a generic list of
> common code terms in Turkish, English and Spanish.

## Features

- **SMS detection** — listens for incoming SMS messages containing a code.
- **Notification detection** — listens for app notifications containing a code.
- **Auto-copy** — detected codes are copied to the clipboard automatically.
- **Drag-and-drop bubble** — when a code arrives (or any 4–8 digit number
  is copied anywhere on the device), a bubble appears on screen:
  - Drag the bubble to move it out of the way.
  - Long-press the code and drag it onto a text field to paste it there
    (if the field accepts drops).
  - Tap the code to copy it again. × closes the bubble. It auto-closes
    after 90 seconds.
- **Clipboard watching** — a bubble also appears for any number copied
  anywhere on the device, not only for detected codes.
- **Notifications** — a notification shows the code, with **Copy** and
  **Ignore** actions.
- **History** — the last 50 detected codes, with time, source and message
  snippet. Tap **Copy** to copy again, **Delete** to remove one entry.
- **Editable sensitive phrases** — the phrase list that marks a message as
  a code message can be edited in the app (one phrase per line) or reset
  to defaults.
- **Ignored apps and senders** — ignore codes from selected apps
  (package names) or SMS senders.
- **Broadcast** — optionally broadcasts detected codes
  (`com.bmo.kodbalonu.CODE_DETECTED`) for automation apps.
- **Sensitive clipboard** — optionally marks the clipboard entry as
  sensitive (Android 13+).
- **Hide code in history** — optionally masks codes in the history list.
- **Auto-clean** — optionally deletes history entries older than 30 days.
- **Spaced and hyphenated codes** — codes written as `123 456` or `123-456`
  are normalized and detected.
- **12 languages** — English (default), Turkish, Spanish, German, French,
  Italian, Portuguese, Russian, Arabic, Hindi, Indonesian and Chinese.
  Choose **System language** in the app to follow the phone's language.

## Permissions

| Permission | Why |
|---|---|
| SMS (receive/read) | Detect codes in incoming SMS messages. |
| Notification access | Detect codes in app notifications. |
| Display over other apps | Show the bubble above other apps. |
| Foreground service | Keep clipboard watching running. |
| Post notifications | Show detected codes as notifications. |
| Accessibility (optional) | Fallback clipboard watching on Android versions where a foreground service cannot read the clipboard in the background. It does **not** read window content — it only checks the clipboard. |

## Privacy

- All data stays on the device. There is **no internet permission** and
  no network code, no tracking, no analytics, no ads.
- History is stored locally only and can be cleared in the app at any time.

## Build

No Gradle is required. You need JDK 17 and the Android SDK
(platform 34, build-tools 34.0.0):

```sh
export KODBALONU_KS_PASS="your-keystore-password"
./build.sh
```

The script compiles resources with `aapt2`, Java with `javac`, dexes with
`d8`, then zipaligns and signs with `apksigner`. If no keystore exists,
one is generated on first run. The keystore is **not** included in this
repository — generate your own for your own builds.

## Project layout

```
proj/
  AndroidManifest.xml
  src/com/bmo/kodbalonu/
    MainActivity.java            # UI: permissions, settings, phrases, history
    Detector.java                # phrase matching + code extraction
    SmsReceiver.java             # incoming SMS
    CodeNotificationListener.java# app notifications
    CodeHandler.java             # copy, bubble, notification, broadcast
    BubbleManager.java           # draggable overlay bubble
    ClipboardService.java        # clipboard watching (foreground service)
    ClipboardAccessibilityService.java  # optional fallback watcher
    ClipboardHelper.java         # clipboard copy (+ sensitive flag)
    HistoryStore.java            # local history (last 50)
    CopyReceiver.java            # notification "Copy" action
    IgnoreReceiver.java          # notification "Ignore" action
    BootReceiver.java            # restart watching after reboot
    LangHelper.java              # language selection
  res/
    drawable/ic_launcher.png     # app icon (AI-generated, see icon.png)
    values/strings.xml           # English (default)
    values-tr/ values-es/ values-de/ values-fr/ values-it/
    values-pt/ values-ru/ values-ar/ values-hi/ values-id/
    values-zh/                   # translations
    xml/accessibility_service_config.xml
releases/kodbalonu-3.0.apk      # signed release APK
```

## Known limitations

- Some text fields (especially some banking and web fields) do not accept
  drag-and-drop codes. In that case tap the bubble to copy, then long-press
  the field and paste.
- On newer Android versions a foreground service may not be able to read
  the clipboard in the background; enable the optional accessibility
  service in the app if the bubble never appears on copy.

## License

MIT — see [LICENSE](LICENSE). Copyright (c) 2026 unsupportedx.
