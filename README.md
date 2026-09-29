# Towfik Serial Platform

This is a **new native Android app**, not a modification of the old compiled Flutter APK. It opens directly to a new dark-themed dashboard with Home, Discover, Watchlist and About screens. There is no Play Store redirect, forced-update code, old `libapp.so`, or old app bundled in the build.

The old APK did not include editable source or a documented content API. This new app does **not** include the old episodes, backend, accounts or functional catalogue; Discover and Watchlist clearly show their empty states. To make those features work, supply a permitted content API and specifications.

**Build:** Actions → Build Towfik Serial Platform APK → Run workflow. Download the `Towfik-Serial-Platform-APK` artifact. You can also run `gradle :app:assembleRelease` with Android SDK and JDK 17 installed.

The new package ID is `com.towfik.serialplatform`, so it installs separately from the old app and cannot update it or import its data. For updates of *this* new app across workflow runs, set repository secrets `APK_KEYSTORE_BASE64`, `APK_KEYSTORE_PASSWORD`, `APK_KEY_ALIAS`, and `APK_KEY_PASSWORD`. Keep a private backup of the signing keystore. Without these secrets, a temporary signing key is generated on each run, making each APK installable but unable to update a previous build.
