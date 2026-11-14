
# BUILD GUIDE — AETHERFALL

## Requirements

- JDK 17
- Gradle 9.3.1
- Android SDK Platform 36.1
- Android Build Tools 36.1.0
- Android Gradle Plugin 9.1.1
- Kotlin 2.2.10

## Build Debug APK

Open terminal in project root.

Run:

```bash
gradle --no-daemon :app:testDebugUnitTest
```

Then:

```bash
gradle --no-daemon :app:assembleDebug
```

APK location:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build Release AAB

Prepare an upload keystore with alias:

```text
upload
```

Set environment variables:

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=your_store_password
export KEY_PASSWORD=your_key_password
```

Run:

```bash
gradle :app:bundleRelease
```

Output:

```text
app/build/outputs/bundle/release/app-release.aab
```

## GitHub Actions

The repository includes:

```text
.github/workflows/android-build.yml
```

Every push to main triggers:

1. Java setup.
2. Gradle setup.
3. Android SDK setup.
4. Project verification.
5. Unit testing.
6. Debug APK compilation.
7. APK validation.
8. Artifact upload.

Release AAB is optional and requires signing credentials.

For GitHub Actions release signing, configure these repository secrets:

- ANDROID_KEYSTORE_BASE64
- STORE_PASSWORD
- KEY_PASSWORD

The debug APK does not require a release keystore.

## Download APK

Open the repository's Actions page.

Select the successful Android APK build.

Download the artifact named:

```text
Aetherfall-Debug-APK
```
