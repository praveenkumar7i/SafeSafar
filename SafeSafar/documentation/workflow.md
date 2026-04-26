# Application Workflow & Architecture

## Architecture
- **Language:** Kotlin
- **UI:** XML Layouts with Material Design 3
- **Background Tasks:** Foreground Service for monitoring Shake and Location.
- **Location:** FusedLocationProviderClient

## Features Workflow
1. **App Launch:** User grants necessary permissions.
2. **Foreground Service:** Starts immediately.
3. **SOS Trigger:** 
   - User taps SOS button OR shakes the device 3 times.
   - `SosService` receives the trigger.
   - Device vibrates.
   - Location is fetched and formatted into a Google Maps URL.
   - SMS is dispatched to predefined contacts.
   - MediaRecorder starts capturing 15 seconds of audio.
