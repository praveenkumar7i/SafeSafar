# SafeSafar - Women Safety App

## Overview
SafeSafar is a complete Android application built to ensure women's safety during emergencies. It features a modern Material 3 UI and works efficiently even in low-internet and low-battery conditions.

## How to Open in Android Studio
1. Extract the `SafeSafar.zip` file.
2. Open Android Studio (Dolphin or later recommended).
3. Click on **Open** (or **File > Open**).
4. Select the extracted `SafeSafar` folder.
5. Wait for Gradle to Sync completely.

## How to Run on Real Device (USB Debugging)
1. On your Android phone, go to **Settings > About Phone**.
2. Tap **Build Number** 7 times to enable Developer Options.
3. Go back to Settings and open **Developer Options**.
4. Enable **USB Debugging**.
5. Connect your phone to your PC using a USB cable.
6. In Android Studio, select your device from the drop-down menu near the Run button.
7. Click the **Run** button to install and launch the app.

## Features
- **SOS Trigger:** Triple shake the phone to activate SOS.
- **Location Tracking:** Uses Fused Location Provider.
- **Offline SMS Fallback:** Sends SMS to emergency contacts if internet is down.
- **Audio Recording:** Automatically records 15 seconds of audio.
- **Foreground Service:** Ensures the app remains active in the background.
