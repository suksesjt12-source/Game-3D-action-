
# AETHERFALL — ANDROID BUILD GUIDE

## Project Information

Name: Aetherfall: The Lost Frontier

Platform: Android

Language: Kotlin

Rendering: OpenGL ES 3.0

UI: Jetpack Compose

Build System: Gradle Kotlin DSL

## Required Environment

- JDK 17
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- Android SDK Platform 36.1
- Android SDK Build Tools 36.0.0
- Kotlin 2.2.10

## 1. Build Debug APK

From the repository root:

```bash
gradle --no-daemon :app:testDebugUnitTest
```

After tests:

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Debug APK uses the standard Android Gradle Plugin debug signing configuration.

A custom release keystore is not required.

## 2. Build Release AAB

Prepare a keystore with alias:

```text
upload
```

Set environment variables:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
```

Build:

```bash
gradle --no-daemon :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## 3. GitHub Actions Build

Workflow location:

```text
.github/workflows/android-build.yml
```

The workflow performs:

1. Repository checkout.
2. JDK installation.
3. Android SDK installation.
4. Gradle installation.
5. Project structure validation.
6. Kotlin unit tests.
7. Debug APK compilation.
8. APK output validation.
9. APK artifact upload.
10. Optional release AAB compilation.

## 4. Download APK Using Android Phone

1. Open the GitHub repository.
2. Open the Actions tab.
3. Select Aetherfall Android APK Build.
4. Open the successful workflow run.
5. Find Artifacts.
6. Download Aetherfall-Debug-APK.
7. Extract the downloaded ZIP.
8. Install app-debug.apk.

## 5. Release Signing Secrets

For release AAB generation, configure these repository secrets:

- ANDROID_KEYSTORE_BASE64
- STORE_PASSWORD
- KEY_PASSWORD

ANDROID_KEYSTORE_BASE64 must contain the Base64 representation of the upload keystore.

Without these secrets, debug APK generation continues normally and release generation is skipped.

## 6. Important Build Notes

This repository currently uses Gradle 9.3.1.

The workflow installs that exact Gradle version directly because the repository does not currently contain the Gradle wrapper executable and JAR.

Do not run ./gradlew until the wrapper files have been generated.

Do not remove the existing application source files.

Do not remove the OpenGL renderer, game simulation, UI, save system, or game assets when repairing build configuration.
