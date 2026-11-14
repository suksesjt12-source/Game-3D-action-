# BUILD GUIDE — AETHERFALL: THE LOST FRONTIER

## Prerequisites

- **Android SDK** (Compile SDK 36, Min SDK 24 / Android 7.0+, Target SDK 36)
- **JDK 17** or **JDK 11**
- **Gradle** (configured in `gradle/wrapper/gradle-wrapper.properties`)

## Building Debug APK (Testing)

From the repository root:

```bash
gradle :app:assembleDebug
```

Output APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Building Release APK / Android App Bundle (AAB)

1. Generate or provide a release upload keystore (`my-upload-key.jks`) with alias `upload`.
2. Export signing credentials as environment variables:
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD=your_store_password
   export KEY_PASSWORD=your_key_password
   ```
3. Run:
   ```bash
   gradle :app:assembleRelease
   gradle :app:bundleRelease
   ```
4. Outputs:
   - Release APK: `app/build/outputs/apk/release/app-release.apk`
   - Release AAB: `app/build/outputs/bundle/release/app-release.aab`

## Automated CI Build

See `.github/workflows/android-build.yml` for the automated GitHub Actions pipeline that verifies project structure, executes unit/Robolectric tests, builds the APK/AAB, and uploads the APK artifact.
