# FASILHD

Native Android file analysis and organization application written in Kotlin using Android Views.

## Project configuration

- Package / Application ID: `com.example.zipstats`
- Minimum SDK: 24
- Compile SDK: 35
- Target SDK: 35
- JDK: 17
- Android Gradle Plugin: 8.7.3
- Gradle: 8.9
- Kotlin: 2.0.21
- Storage access: Android Storage Access Framework (SAF)

## Current UI

The application uses a native Android Views interface with:

- FASILHD branded top app bar
- RTL Arabic / English switching
- Light / dark theme switching
- Native vector icons
- Dashboard with live file/folder/size metrics
- Storage/account status analysis
- Duplicate detection and cleanup actions
- Rules-based cleanup
- File comparison
- Statistics and favorites
- Search and tools
- Real-device folder scanning through SAF
- TeraBox visual entry point without a fake API/login integration
- Multi-selection, copy/move/delete, rename and favorites
- JSON, text report and CSV exports

## Build from Termux

Use JDK 17 and Gradle 8.9:

```bash
gradle --no-daemon :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions

The workflow at `.github/workflows/android-apk.yml` builds the debug APK using JDK 17, Android SDK 35 and Gradle 8.9.

## Safety notes

- No `MANAGE_EXTERNAL_STORAGE` permission is used.
- SAF is used for user-selected files and folders.
- Virtual JSON/ZIP edits modify the loaded in-memory structure until exported.
- Real-folder operations can modify actual device files and destructive operations require confirmation.
- TeraBox is currently presentation-only; no undocumented API or fake connection is included.
