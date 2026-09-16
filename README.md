# NutriFit AI (FitnessApp) 🥗💪

An offline-first, privacy-focused Android fitness companion and dietary nutrition tracker with cutting-edge Gemini Vision AI and local peer-to-peer Wi-Fi synchronization for couples and workout partners.

---

## 🌟 Highlights

- **Gemini 3.8 Flash Vision AI**: Multimodal food photograph recognition specializing in global and Indian cuisine (accurately counts rotis, dals, sabzis, estimating hidden cooking oils and macronutrients).
- **Dynamic Model Discovery & Multi-Tier Fallback**: Automatically discovers enabled models via Google's `v1beta` catalog, prioritizing `gemini-3.8-flash` with resilient automatic fallback (`gemini-3.7-flash` ➔ `gemini-3.5-flash` ➔ `gemini-2.0-flash` ➔ `gemini-1.5-flash` ➔ offline mock) on rate limits (HTTP 429).
- **Zero-Cloud Local Wi-Fi Sync**: Synchronize nutrition logs, weights, and daily progress between partners on the same home Wi-Fi or mobile hotspot using multi-vector discovery (Android NSD/mDNS, fast parallel subnet scanning, and direct IP connection).
- **Partner Duel Dashboard**: Track healthy habits collaboratively with real-time compliance scores, friendly competition, and peer cheering.
- **Scientifically Grounded Nutrition Engine**: Mifflin-St Jeor equation for Basal Metabolic Rate (BMR) and Total Daily Energy Expenditure (TDEE) with customizable calorie deficit levels and WHO/Asian BMI thresholds.
- **Private Transformation Gallery**: Biometric & PIN-locked private photo gallery to track physique progress over time.
- **100% Privacy by Design**: Zero cloud server dependencies. All personal logs, photos, and metrics stay on your physical device. API keys are entered locally by the user and never committed or transmitted to any third-party server other than Google's official Gemini endpoint.

---

## 🏛️ System Architecture

NutriFit AI is built with modern Android architectural best practices following Clean Architecture and unidirectional data flow (UDF):

```mermaid
graph TD
    subgraph UI Layer ["UI Layer (Jetpack Compose & Material 3)"]
        Nav["Navigation Host"]
        Dashboard["Main / Dashboard"]
        Scanner["CameraX Food Scanner & Review Dialog"]
        Partner["Partner Duel & Wi-Fi Sync"]
        Profile["Profile Setup & BMI Engine"]
        Gallery["Private Transformation Gallery"]
    end

    subgraph Domain Layer ["Domain Layer (Business Logic & Calculations)"]
        NutrEngine["NutritionEngine (Mifflin-St Jeor TDEE)"]
        BmiCalc["BmiCalculator (WHO & Asian Classifications)"]
    end

    subgraph Data Layer ["Data Layer (Repository & Persistence)"]
        Repo["FitnessRepositoryImpl"]
        SharedPrefs["Encrypted / SharedPreferences Local Storage"]
    end

    subgraph Network & Sync ["Network & Distributed Sync Engine"]
        GeminiService["GeminiVisionService (Dynamic Discovery & 429 Cascading)"]
        KtorServer["Embedded Ktor HTTP Server (Port 8988)"]
        KtorClient["LocalSyncClient (Raw TCP Probes & HTTP Client)"]
        NsdManager["Android NSD / mDNS Discovery"]
        SubnetScanner["Parallel Subnet Scanner (Semaphore 25)"]
    end

    UI Layer --> Domain Layer
    UI Layer --> Data Layer
    Data Layer --> Domain Layer
    Data Layer --> Network & Sync
    Network & Sync --> GoogleAI["Google Gemini API (v1beta REST)"]
    Network & Sync <--> PeerDevice["Partner Device (Local LAN)"]
```

### Layer Breakdown

1. **Presentation Layer (`com.example.fitnessapp.ui.*`)**
   - 100% Jetpack Compose with Material Design 3.
   - Reactive UI driven by Kotlin `StateFlow` and Compose `remember` / `derivedStateOf`.
   - CameraX integration with tap-to-focus, pinch-to-zoom, gallery photo picker, and meal time tagging.
2. **Domain Layer (`com.example.fitnessapp.domain.*`)**
   - **`NutritionEngine`**: Calculates BMR using the Mifflin-St Jeor formula, applies activity multipliers, and subtracts calorie deficits (Mild: -250 kcal, Moderate: -500 kcal, Aggressive: -750 kcal, or custom).
   - **`BmiCalculator`**: Computes BMI with standard WHO and Asian risk-adjusted thresholds.
3. **Data Layer (`com.example.fitnessapp.data.*`)**
   - Single source of truth via **`FitnessRepository`**.
   - Offline-first JSON serialization with `kotlinx.serialization`.
   - Thread-safe in-memory caching backed by persistent Android SharedPreferences.
4. **Local Sync Architecture (`com.example.fitnessapp.data.sync.*`)**
   - Embedded lightweight **Ktor HTTP Server** listening on port `8988`.
   - **Multi-Vector Discovery Engine**:
     1. *Vector 1 (mDNS)*: Android `NsdManager` (`_fitnessduel._tcp.`) with serialized resolve queue.
     2. *Vector 2 (Subnet Scan)*: Parallel `/24` TCP probes completed in ~2s, bypassing router client isolation.
     3. *Vector 3 (Direct IP)*: Fail-safe manual IP entry.
   - Exchange protocol validates a shared **Pair Code** and exchanges signed nutrition summaries and cheers without sending photos or keys across the wire.
5. **AI Vision Pipeline (`com.example.fitnessapp.data.network.GeminiVisionService`)**
   - Resizes photos up to 1024px and encodes to JPEG Base64.
   - Injects structured nutrition prompts and prompts for Indian dishes (counting Rotis, Dals, Modaks, ghee/oil estimation).
   - Enforces relaxed safety settings (`BLOCK_ONLY_HIGH`) to avoid false positives on food imagery.
   - Extracts clean JSON even when models respond with conversational preambles or code fences.

---

## 🔒 Security & Privacy

- **Zero Hardcoded Secrets**: No API keys or credentials exist in the codebase.
- **User-Provided Keys**: Gemini API keys are configured via **Settings** by the user and stored securely on the local device.
- **LAN-Only Sync**: Wi-Fi synchronization runs strictly over local network sockets without routing through any external cloud server.
- **Biometric Security**: Transformation body photos are protected behind optional biometric / PIN authentication.

---

## 🛠️ Tech Stack & Dependencies

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose, Material 3
- **Camera**: AndroidX CameraX (Camera2 integration)
- **Image Loading**: Coil Compose
- **Networking**: OkHttp 4, Ktor Client & Embedded Server (CIO engine)
- **Serialization**: `kotlinx.serialization.json`
- **Concurrency**: Kotlin Coroutines & Flow
- **Build System**: Gradle with Kotlin DSL (`build.gradle.kts`)
- **Compatibility**: Android 8.0 (API level 26) through Android 15 (API level 35)

---

## 🚀 Getting Started

### Prerequisites

1. Android Studio Ladybug (or newer) or Android SDK with platform tools `34`+.
2. JDK 17.
3. A Google Gemini API Key from [Google AI Studio](https://aistudio.google.com/) (Free Tier available).

### Building from Source

```bash
# Clone the repository
git clone https://github.com/pramodnair/FitnessApp.git
cd FitnessApp

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Installing on Connected Device

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Configuring Gemini AI Vision

1. Open the app on your Android device.
2. Complete the initial onboarding profile or navigate to the **Settings** tab.
3. Scroll to **Google Gemini Vision API Key**.
4. Paste your key from [Google AI Studio](https://aistudio.google.com/) and tap **Verify API Key**.
5. The app will connect and report:
   > *"Connected successfully! Active model: Gemini 3.8 Flash (Latest Flagship)"*

---

## 👥 Couple & Partner Setup

1. Connect both devices to the same home Wi-Fi network (or connect one device to the other's mobile hotspot).
2. Open the **Partner Duel** tab on both devices.
3. Ensure both devices share the same **Pair Code** (e.g. `FIT-8842`).
4. The devices will automatically discover each other. If your Wi-Fi router blocks multicast (mDNS), tap **"Scan Subnet"** or tap **"Direct IP"** and type the IP displayed on the other device.
5. Tap **Sync** to share daily progress, compare streaks, or send motivational cheers!

---

## 📄 License

This project is licensed under the MIT License.
