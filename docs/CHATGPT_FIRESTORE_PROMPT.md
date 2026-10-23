# Prompt for ChatGPT: connect Vortix to Firestore

Copy everything in the box below into ChatGPT, then paste `Repo.java` and `Models.java` when it asks.

```
You are a senior Android engineer (Java only, no Kotlin). I have a working native Android chat app called Vortix (package com.vortix.chat, minSdk 26, compileSdk 34, built on a phone in Termux with Gradle 8.13). It is a Discord + Telegram style app with servers, channels, direct messages, reactions, replies, pins, profiles with levels, themes and sounds.

Today ALL data goes through one class, Repo.java (SQLite). Every screen only calls Repo and listens with Repo.Listener keys: "msg:<channelId>", "servers", "channels", "dms", "reads", "profile". Models.java holds the plain data classes.

TASK: replace the INTERNALS of Repo.java with Firebase Auth (email/password; map username to username@vortix.app) + Cloud Firestore with real-time snapshot listeners, so chat works live across devices. Keep every public method signature, return type and listener key unchanged so I do not have to edit any UI file.

Important design rule: Repo methods are synchronous and return lists. Serve reads from an in-memory cache that snapshot listeners keep fresh, do writes asynchronously with an optimistic local update, and fire the same Repo.Listener keys when the cache changes.

Please give me:
1. Gradle changes (google-services plugin, firebase-bom, firestore, auth) that work in Termux, and where to put google-services.json.
2. The complete new Repo.java (full file, not snippets) and any new helper files.
3. Firestore data model for: users, servers, members, channels, messages, reactions, reads, dms. Include the composite indexes needed.
4. Firestore security rules (only members can read or write a server; users edit only their own profile and messages).
5. Image upload for Message.attach and User.avatar (Firebase Storage needs a paid plan, so also suggest a free option) replacing the local file paths.
6. Offline persistence, and removal of the local demo bots and demo contacts.
7. A short test checklist.

Constraints: no emoji anywhere, no Kotlin, do not touch theme, sound or icon code, keep the APK small, return complete files and list exactly which files changed.
```
