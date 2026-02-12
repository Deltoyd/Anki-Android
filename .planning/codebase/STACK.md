# Technology Stack

**Analysis Date:** 2026-02-12

## Languages

**Primary:**
- Kotlin 2.2.10 - Main application code, all modules
- Java 17 - Target compatibility for JVM compilation

**Secondary:**
- Rust - Backend business logic (via rsdroid library)
- Protocol Buffers - Protobuf Kotlin Lite 4.33.4 for backend message serialization

## Runtime

**Environment:**
- Android Runtime (ART) - Target SDK 35, Min SDK 24
- JVM 21-25 (strict requirement enforced in build.gradle.kts)

**Package Manager:**
- Gradle 9.0.0 (Android Gradle Plugin)
- Gradle version enforced through AndroidGradlePlugin version constraint
- Lockfile: gradle/wrapper/gradle-wrapper.properties

## Frameworks

**Core:**
- AndroidX Suite - Latest stable versions across multiple components:
  - androidx.activity 1.12.2
  - androidx.appcompat 1.7.1
  - androidx.fragment 1.8.9
  - androidx.lifecycle 2.10.0
  - androidx.work 2.11.0 (background tasks, sync)
  - androidx.viewpager2 1.1.0
  - androidx.constraintlayout 2.2.1
  - androidx.sqlite 2.6.2 (database)
  - androidx.webkit 1.15.0

**Coroutines:**
- Kotlin Coroutines 1.10.2 - Async/await patterns throughout codebase
- Experimental Coroutines API enabled via `-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi` compiler flag

**Media:**
- androidx.media3.exoplayer 1.9.0 - Audio/video playback
- androidx.media3.exoplayer-dash 1.9.0 - DASH streaming support

**Serialization:**
- Kotlin Serialization JSON 1.9.0 - JSON serialization
- Protocol Buffers (protobuf-kotlin-lite) 4.33.4 - Backend message format

**Testing:**
- JUnit 5 (Jupiter) 6.0.2 - Test framework (primary, newer tests use this)
- JUnit 4 (Vintage) engine 6.0.2 - Legacy test support
- Robolectric 4.16 - Android unit tests without emulator
- Espresso 3.7.0 - UI/integration testing
- Mockito 5.2.0 - Mocking framework
- Mockito-kotlin 6.2.1 - Kotlin DSL for Mockito
- MockK 1.14.7 - Alternative Kotlin mocking
- Roborazzi 1.56.0 - Screenshot testing
- Cash Turbine 1.2.1 - Flow testing

**Build/Dev:**
- Ktlint 1.8.0 - Kotlin linting (gradle plugin 14.0.1)
- Slack Keeper 0.16.1 - ProGuard rule validation
- Triplet Play 3.13.0 - Google Play Store integration (gradle-play-publisher)
- LeakCanary 2.14 - Memory leak detection (debug builds)
- Gradle plugins from org.jetbrains.kotlin:
  - kotlin-android
  - kotlin-parcelize (Parcelable serialization)
  - kotlin-jvm
  - kotlin-serialization

## Key Dependencies

**Critical (Backend):**
- io.github.david-allison:anki-android-backend 0.1.63-anki25.09.2 - Rust backend for spaced repetition engine
- io.github.david-allison:anki-android-backend-testing - Testing utilities for backend

**Networking:**
- okhttp3 5.3.2 - HTTP client for all network operations
- jsoup 1.22.1 - HTML parsing (addon downloads)
- nanohttpd 2.3.1 - Lightweight HTTP server (media serving)

**Analytics & Crash Reporting:**
- ch.acra:acra-* 5.13.1 - ACRA crash reporting framework:
  - acra-limiter - Rate limiting for reports
  - acra-toast - Toast notifications
  - acra-dialog - Crash report dialog UI
  - acra-http - HTTP sender for reports
- com.brsanthu:google-analytics-java7 2.0.13 - Google Analytics (with OkHttp client)

**Logging:**
- com.jakewharton.timber:timber 5.0.1 - Logging abstraction
- com.arcao:slf4j-timber 3.1 - SLF4J bridge to Timber

**Utilities:**
- commons-io 2.21.0 - File utilities
- commons-collections4 4.5.0 - Extended collections (SetUniqueList)
- commons-compress 1.28.0 - Archive handling
- org.json:json 20251224 - JSON parsing
- com.github.zafarkhaja:java-semver 0.10.2 - Semantic versioning

**UI Components:**
- com.google.android.material:material 1.13.0 - Material Design components
- androidx.constraintlayout:constraintlayout 2.2.1 - Flexible layouts
- com.drakeet.drawer:drawer 1.0.3 - Drawer implementation
- com.github.mrudultora:Colorpicker 1.2.0 - Color picker widget
- com.vanniktech:android-image-cropper 4.7.0 - Image cropping
- com.github.ByteHamster:SearchPreference 2.7.3 - Searchable preference UI

**Desugaring:**
- com.android.tools:desugar_jdk_libs_nio 2.1.5 - Java 9+ API support for older Android

**Annotations:**
- com.google.auto.service:auto-service 1.1.1 - Service provider annotation processor
- org.jetbrains:annotations 26.0.2-1 - Jetbrains nullability annotations

**Testing Utilities:**
- androidx.test:* - AndroidX testing framework
- io.github.ivanshafran:shared-preferences-mock 1.2.4 - SharedPreferences mock
- com.squareup.leakcanary:leakcanary-android 2.14 - Memory leak detection

## Configuration

**Environment:**
- `.env` file not used - Configuration via SharedPreferences and local.properties
- `local.properties` optional file for developer settings:
  - `enable_coverage=false/true` - JaCoCo test coverage
  - `enable_languages=false/true` - Only build English strings (faster)
  - `enable_leak_canary=false/true` - Enable/disable leak detection
  - `local_backend=true` - Use local Anki-Android-Backend build
  - `fatal_warnings=false` - Allow compiler warnings

**Build Configuration Files:**
- `build.gradle.kts` - Root build configuration with shared settings
- `AnkiDroid/build.gradle` - Main app module
- `gradle.gradle.kts` in modules (api, common, libanki, testlib, vbpd, lint-rules)
- `gradle.properties` - JVM settings, AndroidX configuration
- `gradle/libs.versions.toml` - Centralized version catalog (TOML format)
- `settings.gradle.kts` - Gradle modules configuration

**Network Security:**
- `@xml/network_security_config` - Custom network security policies
- Cleartext traffic enabled for custom sync servers via manifest configuration

## Platform Requirements

**Development:**
- JVM 21-25 (enforced - build fails outside range)
- Android Studio compatible with AGP 9.0.0
- Git (for pre-commit hooks)

**Minimum Device:**
- Android 7.0 (API 24) minimum
- 1GB heap minimum (tests configured with 1GB-2GB heap)

**Target Deployment:**
- Android 14 (API 35) target SDK
- Multiple CPU architectures: armeabi-v7a, x86, arm64-v8a, x86_64
- Split APKs generated per architecture for production builds
- Universal APK for debug and optional release builds

**Distribution Channels:**
- Google Play Store (play flavor, default)
- Amazon Appstore (amazon flavor)
- F-Droid / GitHub (full flavor)

---

*Stack analysis: 2026-02-12*
