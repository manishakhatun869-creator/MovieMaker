# Towfik Serial Platform APK branding

The supplied ZIP is a **compiled Flutter APK**, not the original source code. This project changes the Android launcher name/icon and the two bundled Flutter logo images; the compiled Dart UI, text, networking and functionality are otherwise unchanged. Text embedded in the compiled Flutter code cannot be reliably renamed without the original Flutter source.

Run **Actions → Build branded APK → Run workflow** and download the `Towfik-Serial-Platform-APK` artifact. On push, the workflow also builds automatically.

## Signing and updates

For a key that persists between builds, configure repository Actions secrets `APK_KEYSTORE_BASE64` (base64-encoded JKS or PKCS12 file), `APK_KEYSTORE_PASSWORD`, `APK_KEY_ALIAS`, and `APK_KEY_PASSWORD`. **Back up the keystore privately.** Without all four secrets, the workflow signs with a new temporary key on every run; that APK is installable, but cannot update a previous build. The original APK's signing key is unavailable, so even with these secrets the rebranded APK cannot update an installation of the original APK in place unless the original signing key is supplied. Uninstall the original first (this may erase app data).

## Startup update prompt and UI

The original compiled `libapp.so` contains the Play Store URL and a remote `app_version_min` check. The branding script now sets Android version metadata to `999.0.0` (code `9990000`) to pass ordinary minimum-version comparisons. **This is not a guaranteed removal of the update prompt**: the compiled Flutter logic and remote server are not under this repository's control. Removing the Play Store URL alone would leave the blocking screen in place. A reliable fix and a real dashboard/UI redesign require the original Flutter/Dart source code (and access to the version-check backend). The existing compiled dashboard remains unchanged.
