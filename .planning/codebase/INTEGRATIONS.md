# External Integrations

**Analysis Date:** 2026-02-12

## APIs & External Services

**Anki Web Sync:**
- AnkiWeb API (ankiweb.net) - Official Anki database synchronization
  - Used through: Rust backend (anki-android-backend library)
  - Sync protocol: Binary protobuf messages via backend

**Google Analytics:**
- Google Analytics - Usage metrics and analytics
  - SDK: com.brsanthu:google-analytics-java7 2.0.13
  - Tracking ID: Configurable via resources (R.bool.ga_anonymizeIp)
  - HTTP Client: OkHttp3 (custom OkHttpClientImpl wrapper)
  - Config: GoogleAnalyticsConfig with batching enabled (batch size 1 for accuracy)
  - Initialization: `UsageAnalytics.initialize(context)` in AnkiDroidApp
  - Client ID: Installation.id(context) - unique device identifier
  - Opt-in: Controlled via SharedPreferences key `analytics_opt_in`

**Crash Reporting:**
- ACRA (Application Crash Report for Android) 5.13.1
  - Report Endpoint: `https://ankidroid.org/acra/report`
  - Components:
    - acra-http - HTTP sender to endpoint
    - acra-dialog - Crash report dialog UI (AnkiDroidCrashReportDialog)
    - acra-toast - Toast notifications
    - acra-limiter - Rate limiting (60 second minimum interval)
  - Fields Captured: ActivityManager, SQLiteLog, AnkiDroidApp debug logs, rsdroid errors
  - Configuration: CoreConfigurationBuilder in CrashReportService
  - User Control: Feedback report preferences (ALWAYS/ASK/NEVER)
  - Dialog Activity: `com.ichi2.anki.analytics.AnkiDroidCrashReportDialog`

**Addon Downloads:**
- AnkiWeb Addons (api.ankiweb.net)
  - Client: OkHttpClient via HttpFetcher
  - User-Agent: "Mozilla/5.0 (compatible)" for compatibility
  - Timeout: 30 seconds (CONN_TIMEOUT)
  - Location: `AnkiDroid/src/main/java/com/ichi2/anki/web/HttpFetcher.kt`

## Data Storage

**Databases:**
- SQLite (SQLiteDatabase via Android Framework)
  - Provider: androidx.sqlite:sqlite-framework 2.6.2
  - Location: App private storage (managed by OS)
  - Access: Through Rust backend (rsdroid library)
  - Collection Format: .anki2 binary format (protobuf-based)
  - Thread Safety: Single-threaded serialized access via CollectionManager

**Backup:**
- Local file backups - .colpkg (collection package) files
  - Location: ~/AnkiDroid/ directory (external storage)
  - Format: Compressed collection snapshots
  - Backup Rotation: Configurable limits (see BackupLimitsSettingsFragment)

**Shared Preferences:**
- Android SharedPreferences (default)
  - Location: App private directory
  - Mock available: com.ivanshafran:shared-preferences-mock for testing
  - Accessed via: `context.sharedPrefs()` helper

**Media Storage:**
- Local filesystem only
  - Location: ~/AnkiDroid/collection.media/ (internal to collection)
  - Access: Via Rust backend media manager
  - No cloud storage integration

**Caching:**
- None detected - No distributed caching (Redis, Memcached)
- In-memory caching through Collection object lifecycle

## Authentication & Identity

**Auth Provider:**
- Custom - No OAuth/OpenID integration
- AnkiWeb Server Authentication:
  - Method: Username/password via backend API
  - Protocol: Backend handles all auth (protobuf messages)
  - Storage: Server-side only (no local token storage)
  - Session: Stateless per sync request

**Custom Sync Server:**
- Supported via CustomSyncServerSettingsFragment
  - Configuration: SharedPreferences settings
  - URL: Customizable endpoint
  - Auth: Username/password (passed to backend)
  - Protocol: Same as AnkiWeb (protobuf binary)

**Device Identification:**
- Installation ID: Installation.id(context) - unique per device
- Used for: Analytics tracking, sync session identification

## Monitoring & Observability

**Error Tracking:**
- ACRA 5.13.1 (described above)
- Endpoint: https://ankidroid.org/acra/report
- Format: HTTP POST with exception details

**Logs:**
- Timber (logging abstraction) 5.0.1
- Backends:
  - DebugTree - Development (plaintext output)
  - RobolectricDebugTree - Testing (no-op)
  - ProductionCrashReportingTree - Production (logs via ACRA)
- Environment Variables:
  - `RUST_LOG` - Backend logging control (set at app startup)
  - Typical: "info,anki::sync=debug,anki::media=debug,fsrs=error"
  - `TRACESQL` - Optional SQL statement logging (disabled by default)
- Persisted Logs:
  - Logcat capture via ACRA (500 lines with timestamps)

**Uncaught Exceptions:**
- Custom exception handler installed by UsageAnalytics
- Route: Exception → UsageAnalytics.sendAnalyticsException() → ACRA
- Special Exception Types:
  - BackendException - User-facing localized error messages
  - ManuallyReportedException - User-initiated reports
  - Other exceptions - Logged and crash reported

**WebView Debugging:**
- WebView metrics opt-out (privacy-respecting)
- Android manifest declares: `android.webkit.WebView.MetricsOptOut = true`

## CI/CD & Deployment

**Hosting:**
- Google Play Store (primary distribution)
- Amazon Appstore (alternative with reduced permissions)
- F-Droid / GitHub (open distribution)
- Three product flavors support all channels

**CI Pipeline:**
- GitHub Actions (via .github/workflows/)
- Checks run on all PRs:
  - Kotlin linting (ktlint)
  - Unit tests with JaCoCo coverage
  - Emulator/device tests with coverage
  - CodeQL security analysis
- Reporting: Test results posted to GitHub Actions summary

**Build Publishing:**
- Triple-T gradle-play-publisher 3.13.0
- Publishing Configuration:
  - Service account credentials: `~/src/AnkiDroid-GCP-Publish-Credentials.json`
  - Track: alpha (version-based)
  - Version code management with ABI splits
  - Artifact retention policy for backward compatibility

**Release Management:**
- Version code format: AbbCCtDD (8 digits)
  - A: Major version
  - bb: Minor version
  - CC: Maintenance version
  - t: Build type (0=dev, 1=alpha, 2=beta, 3=release)
  - DD: Build number (00 for releases)
- Minification: ProGuard R8 enabled for release builds
- Code obfuscation: proguard-rules.pro configuration

## Environment Configuration

**Required Environment Variables (CI/CD):**
- `KEYSTOREPATH` - Path to signing keystore
- `KEYSTOREPWD` (or `KSTOREPWD`) - Keystore password
- `KEYALIAS` - Key alias in keystore
- `KEYPWD` - Key password
- `CI` - Set to "true" on GitHub Actions
- `TEST_RELEASE_BUILD` - Set to "true" for release build testing
- `MINIFY_ENABLED` - Control R8 minification (default true)
- `GITHUB_STEP_SUMMARY` - GitHub Actions summary file path

**Runtime Configuration (SharedPreferences):**
- `analytics_opt_in` - Analytics consent flag
- `reportErrorMode` - Crash report preference (ALWAYS/ASK/NEVER)
- `enable_coverage` - Test coverage flag (developer)
- `enable_languages` - Language inclusion (developer)
- `enable_leak_canary` - LeakCanary toggle (developer)

**Build Properties (gradle.properties):**
- `org.gradle.jvmargs=-Xmx3072M` - JVM heap for Gradle daemon
- `org.gradle.parallel=true` - Parallel module builds
- `org.gradle.caching=true` - Build cache enabled
- `org.gradle.configuration-cache.parallel=true` - Config cache optimization
- `org.gradle.vfs.watch=true` - File system watching

**Secrets Storage:**
- Local: Keystore file and local.properties (gitignored)
- CI: GitHub Secrets (referenced via ${{ secrets.* }} in workflows)
- No .env files - All configuration through build system

## Webhooks & Callbacks

**Incoming:**
- None detected - No webhook receivers implemented

**Outgoing:**
- None to external services beyond sync protocol
- Internal: ContentProvider-based API for third-party app integration
  - Contract: com.ichi2.anki.FlashCardsContract
  - Provider: CardContentProvider (exported, documented in api module)
  - Operations: Async queries for notes, decks, schedules, media

**Android Intent Callbacks:**
- Tasker integration: `com.ichi2.anki.DO_SYNC` action
- Text processing: PROCESS_TEXT intent support
- File opening: .apkg, .colpkg, .csv, .tsv, .txt files
- Image occlusion: Image sharing intents
- Deep links: anki:// scheme with x-callback-url support

---

*Integration audit: 2026-02-12*
