# cogxar – Predictive Maintenance Monitor (Android: Java + Kotlin)

A robust, read-only industrial predictive maintenance monitoring Android application. The application tracks telemetry and device-level anomaly flags collected from edge hardware (RP2040-ETH microcontroller board with CH9120 Ethernet transceiver) and streamed via a FastAPI backend.

---

## 1. System Overview & Architecture

```
                  ┌───────────────────────────────────────────────┐
                  │                 Hardware Layer                │
                  │   RP2040-ETH + CH9120 (Edge Anomaly Engine)   │
                  └──────────────────────┬────────────────────────┘
                                         │ HTTP Telemetry
                                         ▼
                  ┌───────────────────────────────────────────────┐
                  │                 Backend Layer                 │
                  │     FastAPI Server + SQLite / PostgreSQL      │
                  │     Read-Only REST Endpoints: /machines       │
                  └──────────────────────┬────────────────────────┘
                                         │ JSON (ISO-8601 UTC)
                                         ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               Android Client (cogxar)                                  │
│                                                                                        │
│  [ UI Layer - Mixed Java & Kotlin with Bottom Navigation (3 Tabs) ]                    │
│  MainActivity ──> NavHostFragment                                                      │
│       ├── DashboardFragment (Java, ViewBinding, LiveData)                              │
│       ├── HistoryFragment   (Kotlin, ViewBinding, StateFlow - Data Log)                │
│       └── TrendsFragment    (Kotlin, ViewBinding - Trends Placeholder)                 │
│                                                                                        │
│  [ Shared State ]                                                                      │
│  SharedMachineViewModel (Activity-scoped, keeps selected machine in sync across tabs)  │
│                                                                                        │
│  [ Pure Java Repository Layer (Single Data Access Boundary) ]                          │
│  PdmRepository (Interface with @NonNull / @Nullable annotations)                       │
│       ├── MockPdmRepository   <─── (When USE_MOCK = true)                              │
│       │        └── MockDataSource (1h, 24h, 7d telemetry, anomalies, edge-case variants)│
│       └── RemotePdmRepository <─── (When USE_MOCK = false)                             │
│                └── RetrofitClient + ApiService (GET /machines, /dashboard, /readings)  │
│                                                                                        │
│  [ Coroutine Bridge (Kotlin) ]                                                         │
│  RepositoryExtensions.kt (suspendCancellableCoroutine wrapping CancellableRequest)     │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Mixed Java / Kotlin Development Rules
1. **Existing Screens Stay Java**: Phase 1 code (Dashboard, Adapters, Model POJOs, Repositories) remains 100% pure Java.
2. **New Screens in Kotlin**: Phase 2 screens (History Fragment, Trends Fragment, ViewModels, Table Adapter, Chart Renderer) are written in Kotlin.
3. **One Language Per Screen**: Screen modules are strictly self-contained in either Java or Kotlin to maintain architectural hygiene.
4. **Repository as Sole Data Boundary**: The repository interface (`PdmRepository`) remains pure Java without Android dependencies. Kotlin coroutine bridges (`getReadingsSuspend`) wrap the underlying callback cleanly.
5. **Activity-Scoped Machine Synchronization**: `SharedMachineViewModel` synchronizes the currently selected machine bidirectionally between Dashboard, History, and Trends.

---

## 2. Navigation & Screen Breakdown

The app features a 3-tab bottom navigation structure:

1. **Dashboard (Tab 1, Java)**:
   - High-level equipment health monitoring.
   - Status indicators (Running, Stopped, Offline, Unknown).
   - Telemetry metric cards (Temperature, Vibration RMS) and anomaly alert banners with single-banner priority.
   - Flush header with clean "cogxar" branding (no placeholder icon).
   - In-app debug scenario switcher (debug builds only).

2. **History (Tab 2, Kotlin)**:
   - Dedicated **Readings Data Log** (tabular telemetry view).
   - Top controls: Machine dropdown (`ExposedDropdownMenu`) and Time-range dropdown (`1 hour`, `1 day`, `7 days`, default: `1 day`).
   - Changing either dropdown triggers automatic cancellation of previous in-flight requests and ignores stale responses.
   - Readings table: sorted newest first, showing local time, monospace temperature, monospace vibration, and textual flag (`ANOMALY` in red, `NORMAL` in green).
   - Expandable pagination via "Show More" / "Show Less".
   - Server Export button: disabled when no data is available; tapping displays a notice that export will be generated by the server once connected (future endpoint: `GET /machines/{id}/export?range=1d&format=xlsx`).

3. **Trends (Tab 3, Kotlin)**:
   - Dedicated placeholder screen styled in the Palette A industrial dark theme (`#14161A`).
   - Prepared to receive multi-sensor telemetry trend charts and historical time-series analytics.
   - Shared machine selection observed via `SharedMachineViewModel`.

4. **Reusable Chart Component (`ChartRenderer.kt` & `ChartUtils.kt`)**:
   - Built with MPAndroidChart using Palette A Graphite & Amber styling (`#14161A` base, `#1D2025` card surface, `#2E3238` grid).
   - Formatted axes: left Y-axis only, 4–5 labels with 1 decimal formatting, offsets preventing overlap, and range-aware X-axis formatting (`HH:mm` for 1h/24h, `MM/dd` for 7d).
   - Anomaly-preserving downsampling (`ChartUtils.downsample`) guarantees every anomaly is preserved while bounding plot density.

---

## 3. Getting Started & Running the Project

### Prerequisites
- **JDK**: Java 17+ (e.g. Eclipse Adoptium OpenJDK 17)
- **Android SDK**: API 35 platform and build-tools installed
- **Gradle**: Gradle 9.1.0+ (managed via included `./gradlew` wrapper)

### Running via Android Studio
1. Open Android Studio and choose **Open an existing project**.
2. Select the repository root: `d:\Projects\Internship\trial\`.
3. Let Gradle sync and index the project.
4. Select a virtual device (e.g. Pixel 8 with API 26+) or physical device.
5. Click **Run** (`Shift + F10`).

### Running via Command Line
```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Build release APK (verifies debug code stripping)
.\gradlew.bat assembleRelease

# Install directly to an attached device / emulator
.\gradlew.bat installDebug
```

---

## 4. Switching Mock Scenarios & Live API

### 4.1 Live In-App Debug Variant Switcher (Debug Builds Only)
In debug builds (`BuildConfig.DEBUG == true`), a settings gear icon (`ic_settings`) appears in the top-right header of the Dashboard:
1. Tap the gear icon to open the **Debug Mock Scenario** dialog.
2. Select any of the 6 simulated scenarios:
   - **Normal (Live Telemetry)**: Device is running, live 5s sensor readings (temp ~72°C, vibration ~4.2 mm/s), no anomalies.
   - **Anomaly Flagged**: Device is running, vibration spikes to 9.8 mm/s, highlighted in the red hazard card.
   - **Device Offline**: Device is offline, demonstrates single-banner priority (offline banner shown, stale banner suppressed).
   - **Stale Data**: Telemetry timestamps older than 30 seconds (`STALE_THRESHOLD_SEC`), showing the amber outdated banner.
   - **No Data**: Machine with zero recorded sensor readings, rendering the empty state.
   - **Server Error**: Simulates a 503 backend failure, rendering the error layout with a **Retry** button.
3. Tap **Apply** to reload the dashboard immediately.

> **Production Isolation Note**: The dialog layout is located in `src/debug/res/layout/dialog_debug_variant_picker.xml` and the controller in `src/debug/java/com/cogxar/pdm/debug/DebugVariantPicker.java`. In release builds (`src/release`), a no-op stub is compiled, and the button is permanently hidden.

### 4.2 Switching to the Live FastAPI Server
To switch from mock data to the real backend:
1. Open `app/build.gradle`.
2. Update the `buildConfigField` parameters:
   ```groovy
   // Switch mock flag to false
   buildConfigField "boolean", "USE_MOCK", "false"

   // Set backend URL (use http://10.0.2.2:8000/ for local emulator testing)
   buildConfigField "String", "BASE_URL", "\"http://10.0.2.2:8000/\""

   // Optional adjustments for polling interval and stale threshold
   buildConfigField "long", "POLL_INTERVAL_MS", "5000L"
   buildConfigField "long", "STALE_THRESHOLD_SEC", "30L"
   ```
3. Sync and build the project.
4. **Cleartext HTTP**: Android blocks plain HTTP by default. A debug network security configuration (`src/debug/res/xml/network_security_config.xml`) is configured to allow cleartext HTTP for `10.0.2.2` and `localhost` during debug builds without compromising release security.

---

## 5. Unit Testing Suite

The project includes pure JVM unit tests spanning both Java and Kotlin layers:

### Test Classes
- **`TimeUtilsTest` (Java)**:
  - Validates ISO-8601 UTC timestamp parsing to epoch milliseconds.
  - Validates deterministic local time formatting across timezones.
  - Validates relative time formatting ("just now", "1 min ago", "15 mins ago", "1 hr ago", "2 days ago").
  - Validates staleness calculations against threshold limits.
- **`MockPdmRepositoryTest` (Java)**:
  - Verifies data integrity across all 6 `MockVariant` modes.
  - Verifies synchronous execution with `Runnable::run` executor.
  - Verifies request cancellation (`CancellableRequest`) aborting callbacks.
  - Verifies stub error returned by `exportReadings`.
  - 100% free of Android runtime classes.
- **`DashboardViewModelTest` (Java)**:
  - Uses `InstantTaskExecutorRule` from `androidx.arch.core:core-testing`.
  - Verifies machine auto-selection and transition into `Success` state.
  - Verifies machine switching and in-flight request cancellation.
  - Verifies staleness and device offline emissions.
- **`ChartUtilsTest` (Kotlin)**:
  - Validates downsampling to <= 200 points.
  - Mathematically verifies that every anomaly point is retained (none dropped).
  - Verifies strict chronological ordering preservation.
- **`HistoryViewModelTest` (Kotlin)**:
  - Uses `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`) and `app.cash.turbine`.
  - Verifies `Loading -> Success(96 points for 24h)` initial flow.
  - Verifies `Empty` state emission under `MockVariant.EMPTY`.
  - Verifies `Error` state emission under `MockVariant.SERVER_ERROR`.
  - Verifies range switching loads correct point counts (`1h` = 60, `7d` = 84).
  - Verifies machine switching reloads and cancels previous fetch.
  - Verifies rapid machine switches ignore stale in-flight results.

### Running Unit Tests
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```
Test results and HTML reports are output to:
`app/build/reports/tests/testDebugUnitTest/index.html`
