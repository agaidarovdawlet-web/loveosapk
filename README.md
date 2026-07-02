# loveosapk

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![Build](https://img.shields.io/badge/Build-GitHub%20Actions-informational)
![Release](https://img.shields.io/badge/Release-v1.0.0-blue)
![License](https://img.shields.io/badge/License-MIT-green)

Relationship companion Android app with shared tasks, wishlist, notes, games, Room, DataStore, and Firebase sync.

## Who This Project Is For

- couples-focused mobile product prototypes;
- recruiters evaluating modern Compose UI and local/mobile architecture;
- clients who need a lifestyle app MVP with sync and offline storage.

## Key Features

- relationship timer and home dashboard;
- cycle / care companion tools;
- wishlist, shared tasks, notes, and savings ideas;
- mini-games and companion features;
- Room-based local storage and DataStore preferences;
- Firebase sync for shared data scenarios.

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Firebase Realtime Database
- Firebase Auth
- Firebase Storage
- Coil 3
- Media3

## Architecture

```text
UI -> ViewModel -> Repository -> Room / DataStore / Firebase
```

## Screenshots

Screenshot folder:

- [docs/screenshots](docs/screenshots/README.md)

Recommended captures:

- home dashboard
- cycle screen
- wishlist / tasks
- notes or games
- profile section

## GIF Or Video Demo

- APK is distributed via GitHub Releases
- short UI demo can be linked later from the release notes

## Installation And Run

```bash
git clone https://github.com/agaidarovdawlet-web/loveosapk.git
cd loveosapk
./gradlew assembleDebug
```

APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Firebase client config example:

- `app/google-services.json.example`

## Project Structure

```text
app/src/main/java/com/example/loveosapk/
├── data/
├── domain/
├── ui/
└── MainActivity.kt
```

## What I Implemented Personally

- Compose-based mobile UI and screen flow;
- local persistence with Room and DataStore;
- Firebase-backed sync structure;
- lifestyle-oriented feature set for a real app concept;
- release workflow for APK delivery.

## Status

Portfolio/demo Android app. Good showcase for Compose UI, local data, Firebase integration, and multi-feature mobile app structure.

## Plans

- add final screenshots and short demo clip;
- improve tests and CI coverage;
- polish release build and signing pipeline.
