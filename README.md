# AA-ReVanced — Android Auto Compatibility Patch

Custom ReVanced patch that makes **any APK** compatible with **Android Auto**.

## What it does

This patch modifies the APK's `AndroidManifest.xml` to inject Android Auto support:

1. **Car app metadata** — Adds `com.google.android.gms.car.application` meta-data pointing to an automotive app descriptor
2. **Automotive app descriptor** — Injects `res/xml/automotive_app_desc.xml` declaring media and notification support
3. **Car dock/mode categories** — Adds `CAR_DOCK` and `CAR_MODE` categories to the main launcher activity so Android Auto can discover and launch the app
4. **Automotive feature flag** — Declares `android.hardware.type.automotive` as optional (won't block phone installs)
5. **Foreground service permission** — Ensures the app can run media playback in the background on Auto

## Usage with ReVanced Manager

1. Build the patches JAR:
   ```bash
   ./gradlew :patches:jar
   ```
2. The output JAR will be at `patches/build/libs/aa-revanced-patches-1.0.0.jar`
3. In **ReVanced Manager** → Settings → Sources:
   - Add this JAR as a custom patches source
4. Select your target APK (e.g., YouTube ReVanced, Spotify, etc.)
5. Enable the **"Android Auto compatibility"** patch
6. Patch and install

## Use case

Perfect for apps like **YouTube ReVanced** (e.g., with ad-blocking patches) that you want to use on your car's Android Auto display when parked.

## Requirements

- ReVanced Manager 1.0+ or ReVanced CLI
- JDK 17+ (for building)
- The target APK must be a media/video app for best results

## Project structure

```
patches/
├── build.gradle.kts
└── src/main/
    ├── kotlin/dev/coupde/patches/
    │   └── AndroidAutoCompatibilityPatch.kt    # The patch logic
    └── resources/android-auto/xml/
        └── automotive_app_desc.xml              # Android Auto descriptor
```

## Notes

- This patch works best with apps that already have media playback capabilities
- Android Auto may require the app to implement `MediaBrowserService` for full media integration
- The patch is non-destructive: it only adds entries, never removes existing ones
- Setting `android:required="false"` on the automotive feature ensures the app still installs normally on phones
