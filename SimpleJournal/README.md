# NoteLog

A minimal local-only Android text notes and logs app. The app display name is **NoteLog**.

## Note types
- Notes: automatic `• ` bullets.
- Log: entries use `yyyy-MM-dd HH:mm - text`; a new empty line starts as `[YYYY-MM-DD HH:MM] -` and is timestamped when text is first entered.
- Check-Box: automatic `☐ ` items; tap the box to toggle `☐`/`☑`.
- Bullets: automatic `• ` items.
- Plain: completely free-form text.

## Storage
On first launch choose a folder using Android's system folder picker. Files are saved as ordinary `.txt` documents in that folder, outside the app's private data area. This means clearing app data or uninstalling the app does not delete the notes. Android may revoke the app's folder permission after uninstall/clear-data, so select the same folder again when reinstalling/reopening.

Note-type metadata is kept in the app preferences rather than creating hidden files in your selected folder. Existing notes can also have their type inferred from their contents. The `.txt` files remain the only user-visible note files created by the app.

## Build
Open the `SimpleJournal` folder in Android Studio. The project uses Android Gradle Plugin 8.7.3 and compile/target SDK 35. No `Theme.DeviceDefault.DayNight.NoActionBar` resource is referenced.

Build with **Build > Build APK(s)**. The debug APK is normally written to `app/build/outputs/apk/debug/app-debug.apk`.
