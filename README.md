# Maahi Assistant (Phase 1)

Maahi is an intelligent, conversational voice assistant for Android, built with Kotlin, Jetpack Compose, and Material 3. Designed from the ground up to replicate modern conversational voice interfaces (like ChatGPT Voice), Maahi streams microphone audio directly to Google's Gemini Multimodal Live API over bidirectional WebSockets and plays back natural neural speech with native low latency.

---

## Highlights & Features

### Real-Time Live Voice Streaming
* **Mic Pipeline:** Captures raw 16kHz, 16-bit PCM mono audio using `AudioRecord` and streams chunks to Gemini in real time over WebSockets (`BidiGenerateContent`).
* **Audio Playback:** Receives 24kHz PCM speech from Gemini and plays it through `AudioTrack` with dynamic output RMS tracking.
* **Server VAD & Barge-In:** Turn taking is handled server-side. If you interrupt Maahi by speaking, playback immediately halts and flushes local buffers.
* **Grounding with Google Search:** Spoken responses are grounded with real-time web facts; the app surfaces an active "Searching…" chip in the Chat tab when search executes.
* **Audio Focus & Routing:** Automatically requests transient audio focus (pausing background media like music or podcasts while Maahi speaks) and supports switching between Phone Speaker and Earpiece.

### Animated Neural Orb
* Drawn entirely with Compose `Canvas` featuring:
  * Obsidian dark core gradient
  * Golden orbiting neural nodes connected by synaptic lines
  * Radiant red-gold outer glowing ring
* Responsive orb states:
  * **Idle:** Gentle breathing oscillation
  * **Connecting:** Warm yellow rotating spinner
  * **Listening:** Energetic red-gold glow reacting to microphone amplitude
  * **Speaking:** Outward shockwave pulses modulated by Gemini's audio output level
  * **Muted:** Cool slate-ice tone
  * **Error:** Crimson alert state
* Tap the orb or the floating center mic button on the bottom bar to start or end the session.

### Clay Day Light Theme
* Clean, ergonomic aesthetic featuring:
  * Light blue background (`#EFF5FA`)
  * Soft rounded white-blue cards (`RoundedCornerShape(20.dp - 28.dp)`)
  * Vibrant blue accent (`#1E88E5`)
  * Soft colored edge glow around the screen perimeter only while a live session is active.
* Responsive layouts designed with `dp`/`sp` scaling tested for portrait phones (360dp to 430dp+).
* Outline vector icons converted from clean SVGs across the entire application.

### Multi-Tab Navigation & Drawer
* **Top Bar & Drawer:**
  * Drawer icon and app name "Maahi".
  * Navigation destinations: Home, Chat, Chat history, Help and guide, Settings.
* **Bottom Bar:**
  * Home, Chat, Settings, Yaadein.
  * Floating center microphone FAB to toggle live voice sessions anywhere.
* **Chat Tab:** Live conversation bubbles transcribed in real time from spoken turns or text input.
* **Chat History:** Stored locally in a Room database with clear history confirmation.
* **Yaadein (Memories):** Phase 1 honest empty state ("Coming in a later phase").
* **Help and Guide:** Clear explanations of live voice, barge-in, search grounding, background calling, and earpiece mode.

### Resilient Background Service
* Runs an Android Foreground Service with notification channel `Maahi session`.
* Ongoing notification with quick action buttons: **Mute / Unmute** and **End call**.
* Sessions continue uninterrupted when the app is minimized.
* Automatic network monitoring: detects disconnections, displays `"No connection, retrying…"`, and restores session context using **Gemini Session Resumption** tokens (displaying a `"Reconnected"` note in chat).

### Settings & Security
* **Encrypted API Key Storage:** Stored using `EncryptedSharedPreferences` backed by the Android Keystore.
* **Save & Test Key:** Validates the key with a real minimal REST call to Google AI Studio and displays real API feedback. Includes a "How to get your key" guide and an "Open Google AI Studio" button.
* **Live Model ID:** Configurable model identifier (default: `gemini-3.8-live`).
* **Voice Selection:** Choose between `Kore` (default), `Puck`, `Charon`, `Fenrir`, or `Aoede`.
* **Speaker / Earpiece Switch:** Toggle default audio routing.
* **Keep Screen Awake:** Keeps screen illuminated during active voice sessions.
* **Data Management:** "Reset Settings" (keeps chat) and "Delete All Data" with confirmation dialogs.

---

## Project Structure

```
maahi-assistant/
├── .github/
│   └── workflows/
│       └── android-debug.yml      # CI workflow: builds debug APK on push/dispatch
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml   # Permissions, portrait lock, foreground service
│   │   ├── java/com/maahiassistant/android/
│   │   │   ├── MaahiApplication.kt
│   │   │   ├── data/
│   │   │   │   ├── api/          # Gemini Live WebSocket & REST validator
│   │   │   │   ├── audio/        # AudioRecord 16kHz & AudioTrack 24kHz engines
│   │   │   │   ├── db/           # Room Database & Chat Entities
│   │   │   │   ├── local/        # EncryptedSharedPreferences & DataStore
│   │   │   │   └── repository/   # Chat & Settings repositories
│   │   │   ├── service/          # MaahiSessionService (Foreground Service)
│   │   │   └── ui/               # Compose Screens, Components & Clay Theme
│   │   └── res/                  # Outline VectorDrawables, colors, strings
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Getting Started

### Prerequisites
1. Android SDK (API 26 minSdk, API 35 targetSdk/compileSdk).
2. Java Development Kit (JDK 17).
3. A Google AI Studio Gemini API Key ([https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)).

### Building the Debug APK
```bash
# Clone the repository
git clone https://github.com/mdshahiid09-bit/maahi-assistant.git
cd maahi-assistant

# Build debug APK
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Known Limits (Phase 1)

1. **Gemini Live API Session Duration:** The Gemini Live API WebSocket endpoint has a server-side session duration ceiling (typically 10-15 minutes). Maahi intercepts resumption updates and silently re-establishes the connection, but brief audio pauses may occur during the handshake.
2. **Speech Transcriptions Availability:** `inputAudioTranscription` and `outputAudioTranscription` are configured in the setup handshake. If a particular preview model endpoint or region has transcription processing delays, spoken audio continues to play via `AudioTrack` in real time while text bubbles finalize upon turn completion.
3. **Headless Environment Verification:** Live end-to-end audio roundtrip (mic input -> Gemini model -> speaker output) was verified against official protocol specifications and compiled via CI, but physical acoustics, hardware echo cancellation (AEC), and real mic quality must be experienced directly on a physical handset.
4. **Network Dependency:** Real-time multimodal voice streaming requires a stable low-latency internet connection. Offline speech recognition is intentionally excluded in Phase 1 to prioritize real-time conversational streaming.
5. **Earpiece Routing Differences:** Audio routing to `TYPE_BUILTIN_EARPIECE` is implemented using modern `AudioManager.setCommunicationDevice` (API 31+) and fallback routing, but specific vendor OS skins (e.g. MIUI, OneUI) may enforce strict call-audio policies.
6. **Tool Function Calling:** In Phase 1, tools are focused on Google Search grounding. Custom client-side function calling tools are scheduled for future phases.
7. **Yaadein (Memories):** The Yaadein screen is present as a Phase 1 empty state and will be expanded with persistent memory extraction in later phases.
