# Towfik Serial Platform

A new Android app with a premium dark interface, Home, searchable Serials, paginated Latest episodes, and About. The app opens directly to its dashboard, with no original APK, Play Store redirect, Firebase update check or compiled Flutter files.

## Content service

The original compiled APK referenced `https://scopebd.com/public/ibs_v3/v` with `/serials` and `/videos` routes. This app makes read-only requests to those routes. **On inspection, both endpoints returned HTTP 404**, so the app cannot currently load the catalogue. It displays an error and retry option rather than fake serials. Provide a working authorized API URL and sample JSON responses to connect all serials and episodes reliably. Episode playback is not implemented without a documented playback URL and content rights. The app does not transfer accounts or data from the old APK.

## Build

Actions → Build Towfik Serial Platform APK → download the `Towfik-Serial-Platform-APK` artifact. For update-compatible future releases configure repository secrets `APK_KEYSTORE_BASE64`, `APK_KEYSTORE_PASSWORD`, `APK_KEY_ALIAS`, `APK_KEY_PASSWORD` and keep your keystore private. Without these, each build uses a new temporary signing key, and cannot update the last build. Package ID: `com.towfik.serialplatform` (separate from the old app).
