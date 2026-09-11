# NoteLog 1.4.1

- Renamed the app display name from Simple Journal to NoteLog.
- Removed the duplicate Settings item from the hamburger menu.
- New note flow is now: choose note type first, then enter the filename.
- Made the About page read-only with no developer instructions shown to users.
- Added separate GitHub profile and NoteLog project links in About.
- Removed the missing `applyTheme()` call that caused the Android Studio compile error.

# Changelog

## 1.3.2
- Fixed deletion of notes stored through Android Storage Access Framework (SAF).
- Uses DocumentsContract.deleteDocument() with a compatibility fallback for providers that only implement ContentResolver.delete().
- Incremented versionCode to 5.

# Changelog

## 1.3.1 — Security fix

- Added the Android `USE_BIOMETRIC` permission required by the platform biometric APIs.
- Security is now enabled **only after a successful device authentication**. Cancelling/failing authentication cannot leave the app locked.
- Added defensive fallback/error handling around platform `BiometricPrompt` so unsupported device/ROM combinations do not crash the launcher.
- On unlock failure/cancellation, the app remains on a lock screen with an Unlock button instead of becoming unusable.
- Correctly handles folder selection and authentication results through a single `onActivityResult` implementation.
- Keeps local note files outside app-private storage, so clearing app data does not delete the notes.

# NoteLog V1.3

- Fixed Log editor opening with an extra second line. A new Log opens with exactly one `[YYYY-MM-DD HH:MM] -` placeholder when empty.
- Removed the hidden `.simplejournal_types` metadata file from the selected folder.
- Note types are now stored in app preferences and inferred from file contents when possible.
- Existing `.txt` notes remain ordinary user files; no per-note hidden metadata files are created.
- Autosave avoids rewriting a file on pause when its actual saved content has not changed.
- Kept the Log placeholder out of the saved text until an entry is actually created.
- Retained folder-based Storage Access Framework persistence so notes remain outside app-private storage.

## 1.4.0 — Home UI refresh
- Moved Simple Journal title above the global search bar.
- Replaced the visible Folder/Sort/Lock controls with a clean top-right hamburger settings menu.
- Settings menu includes Folder select, Lock the app, Sort notes, Theme, and About me.
- Added light/dark/system theme selection persisted across launches.
- Added About page with name, thanks note, Buy Me a Coffee and GitHub links.
- Moved Create Note to a larger floating + button at bottom-right.
- Improved responsive home layout while retaining two-column filename/date listing.

## 1.4.3
- Fixed launcher icon resources using adaptive and legacy mipmap icons.
- Hidden provider-trash `.trashed*` files are excluded from the home list.
