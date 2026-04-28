<div align="center">

<img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
<img src="https://img.shields.io/badge/Min%20SDK-21-F44336?style=for-the-badge&logo=android&logoColor=white"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge"/>
<img src="https://img.shields.io/badge/Status-Production%20Ready-4CAF50?style=for-the-badge"/>

<br/><br/>

```
 ____         __       ____         __
/\  _`\      /\ \     /\  _`\      /\ \
\ \,\L\_\    \ \ \    \ \ \/\_\    \ \ \
 \/_\__ \    \ \ \  __\ \ \/_/_    \ \ \  __
   /\ \L\ \   \ \ \L\ \\ \ \L\ \   \ \ \L\ \
   \ `\____\   \ \____/ \ \____/    \ \____/
    \/_____/    \/___/   \/___/      \/___/

```

# 🛡️ SafeSafar

### *Your Safety. Your Control. Anytime. Anywhere.*

**SafeSafar** is a comprehensive personal safety and emergency assistance Android application that automates an entire emergency response with a single gesture — even when your phone is locked.

<br/>

![SafeSafar Banner](https://img.shields.io/badge/🚨%20SOS%20in%20One%20Tap-red?style=for-the-badge)
![Works Offline](https://img.shields.io/badge/📡%20Works%20Without%20Internet-orange?style=for-the-badge)
![Background Service](https://img.shields.io/badge/📳%20Always%20Listening-purple?style=for-the-badge)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [The Problem It Solves](#-the-problem-it-solves)
- [Target Users](#-target-users)
- [Tech Stack & Architecture](#-tech-stack--architecture)
- [Core Features](#-core-features)
- [System Workflow](#-system-workflow-emergency-sequence)
- [UI/UX Design](#-uiux-design-principles)
- [Newly Added Features](#-newly-added-features)
- [How Features Work Offline](#-how-features-work-without-internet)
- [Challenges & Solutions](#-challenges--engineering-solutions)
- [Future Improvements](#-future-improvements)
- [Conclusion](#-conclusion)

---

## 🌐 Overview

> **SafeSafar** is a native Android application built to provide *immediate, automated help* the moment a user feels threatened or is in danger.

In high-stress emergency situations, manually unlocking a phone, finding contacts, dialing numbers, or sending location data is often **impossible**. SafeSafar automates this entire process — allowing users to trigger a full-scale emergency response **discreetly and instantly** via a button tap, phone shake, or power button press.

---

## 🔴 The Problem It Solves

| Situation | Without SafeSafar | With SafeSafar |
|---|---|---|
| User is being followed | Must manually unlock → find contacts → type message → send | One shake triggers GPS alert to all contacts |
| Phone is in pocket | Impossible to access screen | 3x Power Button press fires SOS |
| Attacker grabs phone | User panics, can't navigate | Shake detection fires even while locked |
| No internet connection | Location sharing fails | GPS + SMS works via satellites + cellular |
| Need audio evidence | Open voice recorder manually | Hold-to-record button captures instantly |

---

## 👥 Target Users

- 👩 **Women** traveling alone or in unsafe environments
- 👴 **Elderly individuals** who need quick access to help
- 👦 **Children** or students commuting
- 🌙 **Late-night commuters**
- 🏕️ **Rural travelers** in low-network zones

---

## 🛠️ Tech Stack & Architecture

> Built **natively for Android** in **Kotlin** to ensure maximum hardware integration, reliability, and lifecycle management.

### Core Technologies

| Technology | Purpose |
|---|---|
| ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white) | Primary language — null-safety, coroutines, modern syntax |
| ![XML](https://img.shields.io/badge/XML%20%2B%20Material%20UI-757575?logo=materialdesign&logoColor=white) | Responsive, accessible UI with `MaterialCardView`, `SwitchMaterial` |
| **Foreground Services** | Keeps app alive in background for continuous sensor monitoring |
| **BroadcastReceivers** | Listens to system events like Screen ON/OFF |
| **MediaRecorder API** | Captures audio evidence in `MPEG_4` + `AAC` format |
| **FusedLocationProviderClient** | Accurate, battery-optimized real-time GPS |
| **SmsManager** | Sends SMS directly via telecom network — no internet needed |
| **FileProvider** | Grants secure, temporary access to audio files for external apps |
| **SharedPreferences** | Stores contacts and feature toggle states persistently |
| **SensorManager (Accelerometer)** | Reads motion data to detect phone shake patterns |
| **AudioManager** | Seizes microphone focus from background apps during emergency |
| **Camera2 API** | Controls torch/flashlight for visual SOS alerts |
| **MediaPlayer** | Plays siren/alarm audio |

---

## ⚡ Core Features

### 🚨 1. Instant SOS Trigger (Panic Button)

> The central, massive pulsing red button on the home screen.

- **What it does**: On tap, immediately fetches the user's GPS location and dispatches an emergency SMS to all saved trusted contacts.
- **Tech**: `FusedLocationProviderClient` + `SmsManager` + `MaterialButton`

**Internal Flow:**
```
User Taps Button
      ↓
Verify SEND_SMS & LOCATION Permissions
      ↓
Fetch GPS Coordinates (FusedLocationProvider)
      ↓
Generate Google Maps URL → Format Emergency String
      ↓
SmsManager dispatches SMS to all saved contacts
```

**SMS Format Sent:**
```
"🚨 I am in danger! Help me immediately.
📍 My Location: https://maps.google.com/?q=lat,lng"
```

---

### 📳 2. Shake Detection (Discreet SOS)

> Trigger SOS by shaking your phone **5 times rapidly** — even when locked.

- **What it does**: Background service continuously monitors the accelerometer. 5 consecutive rapid shakes within a tight time window = SOS fires.
- **Tech**: `SensorManager` + `SensorEventListener` + Foreground Service

**Internal Flow:**
```
Foreground Service listens to Accelerometer
      ↓
Calculates G-force deltas continuously
      ↓
If 5 consecutive spikes exceed threshold within time window
      ↓
SOS function invoked automatically
```

> 💡 **Why this matters**: User can shake phone inside pocket without touching the screen — fully discreet.

---

### 🔘 3. Power Button Trigger (3x Press)

> Press the **physical power button 3 times** consecutively to fire SOS.

- **What it does**: Listens for rapid screen ON/OFF state changes. 3 toggles within a timeout = SOS.
- **Tech**: `BroadcastReceiver` → `Intent.ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF`

**Internal Flow:**
```
Screen State Change Detected (ON/OFF/ON/OFF...)
      ↓
Counter increments on each state change
      ↓
If 3 changes occur within timeout window
      ↓
SOS Sequence Fires
```

---

### 🎙️ 4. Audio Evidence Capture (Hold to Record)

> Captures up to **15 seconds** of surrounding audio and shares it via WhatsApp.

- **What it does**: User holds a button to record audio evidence securely to internal storage. On release, it's instantly shareable.
- **Tech**: `MediaRecorder` + `FileProvider` + `AudioManager` + `Handler`

**Internal Flow:**
```
User Holds Record Button
      ↓
AudioManager requests AUDIOFOCUS_GAIN_TRANSIENT
      ↓
200ms delay for hardware initialization
      ↓
MediaRecorder saves recording.mp4 to internal cacheDir
      ↓
User Releases Button
      ↓
stop() → reset() → release() + 500ms buffer
      ↓
FileProvider generates secure temporary URI
      ↓
ACTION_SEND intent opens WhatsApp Share Sheet
```

> 🔒 **Security**: File never written to external storage. `FileProvider` grants temporary read-only access to third-party apps.

---

### 🗺️ 5. Live Location Tracking (Follow Me)

> Real-time map view showing the user's live GPS position.

- **Tech**: Google Maps SDK (`SupportMapFragment`) + Play Services Location
- Allows a trusted contact to visually follow the user's movement in real time.

---

### 📞 6. Auto Fake Call

> Simulates a realistic incoming phone call to help users exit dangerous situations gracefully.

- **What it does**: Displays a convincing caller ID screen with a ringing sound, giving users a social exit strategy.
- **Tech**: Custom Activity + `MediaPlayer` (ringtone) + `Handler` (delays)

---

### 🔊 7. Siren + Flashlight Emergency Alert

> Activates a **loud alarm** + **flashing torch** for 15 seconds during SOS.

- **Siren**: `MediaPlayer` plays user-selected or default alarm sound
- **Flashlight**: `CameraManager` (Camera2 API) toggles torch at rapid intervals

---

### ⚙️ 8. Settings & Battery Optimization

> Full control over which triggers are active, and tools to keep the app alive in background.

- **Tech**: `SharedPreferences` + `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- Users can toggle Shake Detection, Power Button trigger, etc. on/off
- Directs users to disable Android battery optimization for SafeSafar

---

## 🔄 System Workflow (Emergency Sequence)

```
┌─────────────────────────────────────────────────────────────┐
│                    TRIGGER PHASE                            │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  UI Button  │  │  5x Shakes   │  │ 3x Power Button   │  │
│  │    Tap      │  │  (BG Service)│  │ (BroadcastReceiver│  │
│  └──────┬──────┘  └──────┬───────┘  └─────────┬─────────┘  │
│         └────────────────┼──────────────────────┘           │
│                          ▼                                   │
├──────────────────────────────────────────────────────────────┤
│                  CONTEXT GATHERING                           │
│   FusedLocationProvider → High-Accuracy GPS Coordinates     │
├──────────────────────────────────────────────────────────────┤
│                   ALERT DISPATCH                             │
│   Format Emergency String → SmsManager → All Contacts       │
│   Simultaneously: Siren ON + Flashlight ON (15s)            │
├──────────────────────────────────────────────────────────────┤
│                EVIDENCE COLLECTION                           │
│   User holds mic button → MediaRecorder → recording.mp4     │
├──────────────────────────────────────────────────────────────┤
│                  SECURE HANDSHAKE                            │
│   FileProvider → Secure URI → WhatsApp Share Sheet          │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎨 UI/UX Design Principles

### Home Screen
- 🔴 **Massive pulsing red SOS button** — impossible to miss in high stress
- Uncluttered, minimal background — primary action is always in focus
- Designed for **one-handed, zero-cognitive-load** operation

### Settings Interface
- 🃏 **Card-based layout** using `MaterialCardView`
- Soft gradients for a calming, trustworthy aesthetic
- Large icons + high-touch-target toggles for accessibility

### Color Psychology

| Color | Usage | Psychology |
|---|---|---|
| 🔴 Red `#F44336` | SOS Button, emergency actions | Urgency, immediate danger |
| 💜 Deep Purple / Pink Gradient | Secondary UI elements | Trust, calm, modern |
| ⬜ White / Light Gray | Backgrounds | Clarity, focus |

---

## 🔥 Newly Added Features

| Feature | Description |
|---|---|
| 🔔 Custom Siren Sound Selection | Choose your own alarm sound |
| 🔦 Flashlight SOS Alert | Torch flashes during emergency |
| 📩 Offline SMS Support | No internet required — uses cellular |
| 🔁 Auto Retry SOS System | Retries if first SMS fails |
| 📳 Shake Detection (5x) | Shake phone 5 times = SOS |
| ⚡ Power Button Double Press SOS | 3x power press = SOS |
| 🚨 Siren + Flashlight 15s Alert | Combined audio-visual emergency signal |

---

## 📡 How Features Work Without Internet

> **SafeSafar is fully functional with zero internet connection.**

### 📩 SMS Without Internet
```
SmsManager API
      ↓
Uses SIM card + Mobile Network (2G/3G/4G)
      ↓
Direct telecom dispatch — no WiFi needed
```

### 📍 Location Without Internet
```
FusedLocationProviderClient / LocationManager
      ↓
GPS via Satellites (independent of internet)
      ↓
Coordinates formatted into Google Maps URL locally
```

### 🔊 Siren System
```
MediaPlayer
      ↓
Plays from local app assets — no streaming
```

### 🔦 Flashlight Alert
```
CameraManager (Camera2 API)
      ↓
Direct torch mode toggle — hardware level
```

---

## 🧠 Technologies Summary

```kotlin
val techStack = listOf(
    "Kotlin",                    // Primary Language
    "Android SDK",               // Platform
    "SmsManager API",            // Emergency Alerts
    "FusedLocationProviderClient", // GPS Tracking
    "MediaRecorder API",         // Audio Evidence
    "MediaPlayer API",           // Siren
    "Camera2 API",               // Flashlight
    "SensorManager",             // Shake Detection
    "BroadcastReceiver",         // Power Button
    "FileProvider",              // Secure File Sharing
    "SharedPreferences",         // Local Storage
    "Foreground Services",       // Background Monitoring
    "AudioManager",              // Mic Focus
    "Material UI Components"     // Design System
)
```

---

## 🔧 Challenges & Engineering Solutions

### ⚠️ Challenge 1: WhatsApp "File Format Not Supported" Error

| | Detail |
|---|---|
| **Issue** | `.3gp` / `AMR_NB` recordings were rejected by WhatsApp. Releasing recorder too early produced 0-byte corrupt files. |
| **Solution** | Migrated to `MPEG_4` + `AAC` encoders. Implemented strict `stop() → reset() → release()` teardown with a **500ms postDelayed buffer** before opening share intent. |

---

### ⚠️ Challenge 2: Scoped Storage Permission Walls

| | Detail |
|---|---|
| **Issue** | Android 11+ blocks free external storage writes, causing silent failures. |
| **Solution** | Removed `WRITE_EXTERNAL_STORAGE` entirely. Redirected saves to `cacheDir` (internal). Used `FileProvider` to grant temporary read access to third-party apps. |

---

### ⚠️ Challenge 3: Background Service Termination

| | Detail |
|---|---|
| **Issue** | Android battery managers frequently killed the shake detection service. |
| **Solution** | Elevated to **Foreground Service** with persistent notification. Added button to route users to system settings → "Disable Battery Optimization" for SafeSafar. |

---

### ⚠️ Challenge 4: Microphone Hardware Locking

| | Detail |
|---|---|
| **Issue** | Recording crashed if button tapped too fast before hardware initialized, or if another app held mic focus. |
| **Solution** | Added **200ms delayed start buffer** and forced `AudioManager` to request `AUDIOFOCUS_GAIN_TRANSIENT`. |

---

## 🚀 Future Improvements

| Feature | Description |
|---|---|
| ☁️ **Cloud Backup** | Firebase Realtime Database to sync contacts + backup audio off-device |
| 🗺️ **Real-time Web Dashboard** | Live-tracking link sent in SMS for contacts to watch movement |
| 🚔 **Direct API Integration** | Connect to local police or campus security dispatch APIs |
| 🤖 **AI Audio Analysis** | Detect screams or trigger words ("help") automatically — zero physical interaction |
| ⌚ **Wearable Sync** | WearOS companion app to trigger SOS from a smartwatch |

---

## 🌍 Real-World Advantage

```
✅ Works in low network / 2G areas
✅ Works completely without internet
✅ Useful in rural, highway, and remote emergency situations
✅ Discreet — no screen interaction required
✅ Instant — sub-second response from trigger to SMS dispatch
```

---

## 📁 Project Structure

```
SafeSafar/
├── app/
│   ├── src/main/
│   │   ├── java/com/safesafar/
│   │   │   ├── MainActivity.kt          # Home screen + SOS button
│   │   │   ├── ShakeDetectionService.kt # Foreground service (accelerometer)
│   │   │   ├── PowerButtonReceiver.kt   # BroadcastReceiver (screen toggle)
│   │   │   ├── AudioRecorder.kt         # MediaRecorder + FileProvider logic
│   │   │   ├── LocationHelper.kt        # FusedLocationProvider wrapper
│   │   │   ├── SmsHelper.kt             # SmsManager + contact dispatch
│   │   │   ├── FakeCallActivity.kt      # Simulated incoming call screen
│   │   │   ├── LiveMapActivity.kt       # Google Maps live tracking
│   │   │   ├── SettingsActivity.kt      # Toggles + battery optimization
│   │   │   └── SirenFlashHelper.kt      # MediaPlayer + Camera2 flashlight
│   │   ├── res/
│   │   │   ├── layout/                  # XML UI layouts (Material Design)
│   │   │   ├── drawable/                # Icons, gradients, animations
│   │   │   └── raw/                     # Default siren audio
│   │   └── AndroidManifest.xml          # Permissions + service declarations
│   └── build.gradle
└── README.md
```

---

## 🔐 Permissions Required

```xml
<!-- Location -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

<!-- SMS -->
<uses-permission android:name="android.permission.SEND_SMS"/>

<!-- Microphone -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>

<!-- Sensors & Background -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>

<!-- Camera (Flashlight) -->
<uses-permission android:name="android.permission.CAMERA"/>
<uses-feature android:name="android.hardware.camera.flash"/>
```

---

## 🏁 Getting Started

```bash
# 1. Clone the repository
git clone https://github.com/yourusername/SafeSafar.git

# 2. Open in Android Studio (Hedgehog or later)

# 3. Add your Google Maps API Key in local.properties
MAPS_API_KEY=your_api_key_here

# 4. Build & Run on physical device (emulator lacks sensors)
./gradlew assembleDebug
```

> ⚠️ **Important**: Run on a **physical Android device** — shake detection and SMS require real hardware.

---

## 🤝 Contributing

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

---

## 📄 License

```
MIT License — Copyright (c) 2024 SafeSafar

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software to use, copy, modify, merge, publish, and distribute.
```

---

## 🙏 Conclusion

> **SafeSafar is not just an application — it is a vital tool designed to bridge the gap between encountering danger and receiving help.**

By successfully navigating complex Android hardware lifecycle constraints — background sensor persistence, secure file sharing via content providers, real-time location tracking, and offline SMS dispatch — SafeSafar stands as a **robust, production-ready solution** for personal safety.

Its scalable architecture ensures it can continuously evolve to integrate even more advanced preventative technologies in the future.

---

<div align="center">

**Made with ❤️ for Safety**

![Built with Kotlin](https://img.shields.io/badge/Built%20with-Kotlin-7F52FF?style=for-the-badge&logo=kotlin)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Safety First](https://img.shields.io/badge/Safety-First-F44336?style=for-the-badge)

*"Because in an emergency, every second counts."*

</div>
