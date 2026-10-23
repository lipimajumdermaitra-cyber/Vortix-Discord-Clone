# Vortix (Discord + Telegram style chat app)

Native Android, Java only, package `com.vortix.chat`, source zip is well under 1 MB.

## Build in Termux
```
cd Vortix
echo "sdk.dir=$HOME/android-sdk" > local.properties    # change to your SDK path
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
- Needs Android platform 34 (`compileSdk 34`). If you have another platform installed, edit `compileSdk`/`targetSdk` in `app/build.gradle`.
- `gradle.properties` points to Termux's native aapt2. Delete that line if you build on a PC.
- Not compiled or run by the author of this zip (no Android SDK or network was available), so expect to fix a small error or two on the first build and send the log.

## What is inside
| Area | Status |
|---|---|
| Register / login (salted SHA-256, on-device) | Done |
| Servers: create, join by invite code, rail with unread dots | Done |
| Text + announcement channels, create channels | Done |
| Messages: send, reply, edit, delete, pin, copy, @mentions | Done |
| Photo messages (image picker, full screen viewer) | Done |
| Reactions (5 custom icon reactions, no emoji) | Done |
| Discord rows in channels, Telegram bubbles in DMs | Done |
| Telegram-style chat list: search, filters, unread badges, new chat | Done |
| Alerts tab (mentions and replies) | Done |
| Profile: avatar photo, banner, bio, status, XP levels, badges | Done |
| Banner colors unlock at levels 3 and 5 (earned, not bought) | Done |
| 5 themes + 8 accent colors, applied everywhere | Done |
| Sounds: tap, send, receive, notify, join, error (volume + on/off) | Done |
| Custom branding and 37 vector icons, no emoji | Done |
| Real-time updates | Done inside the app, on this phone |
| Chat between different phones | Needs Firestore (see docs/CHATGPT_FIRESTORE_PROMPT.md) |
| Voice/video channels, roles and permissions, threads, push notifications, GIFs | Not included |

## Demo data
Three demo contacts (Vortix, Nova, Pixel) and the server "Vortix HQ" (invite code `VORTIX`) are seeded so the app is not empty. They auto-reply locally. Try `/help` in any channel. Remove them when you add Firestore.

## Firestore later
All data access is in `Repo.java`. Replace its internals only; no screen needs to change. Use the prompt in `docs/CHATGPT_FIRESTORE_PROMPT.md`.
