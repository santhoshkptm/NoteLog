# Building Simple Journal

This revision fixes the original layout problems caused by using raw pixel values where Android layout dimensions require density-independent pixels (dp), and adds explicit system-bar insets for modern Android/Android 15 edge-to-edge behavior.

## Android Studio

1. Open the `SimpleJournal` directory in Android Studio.
2. Let Gradle sync complete.
3. Select **Build > Build APK(s)**.
4. Install the generated debug APK from:
   `app/build/outputs/apk/debug/app-debug.apk`

## Command line

From the `SimpleJournal` directory:

```bash
gradle assembleDebug
```

## GitHub Actions

The included workflow at `.github/workflows/build.yml` builds the debug APK on GitHub Actions. Use **Actions > Build APK > Run workflow**, then download the `SimpleJournal-debug-apk` artifact.
