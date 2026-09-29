# Towfik Serial Platform

Rebranded build of the "Indian Bangla Serial" streaming app.
All branding (app name, launcher icon, splash, in-app logo, UI strings) now
says **Towfik Serial Platform**; everything else works exactly the same.

| | |
|---|---|
| App name | Towfik Serial Platform |
| Package | `com.towfikserial.platform` |
| Version | 1.0.1 (versionCode 3) |
| Min Android | 7.0 (API 24) |
| Signing key | `signing/towfik-release.jks` (alias `towfik`, password `towfik2025key`) |

## Build the APK (update build)

1. Open the **Actions** tab in GitHub.
2. Select **Build Towfik Serial Platform APK** → **Run workflow**.
3. When it finishes, download the APK from the run **artifacts**, or from the
   **Releases** page (`towfik-build-…` release, attached APK).

It also builds automatically on every push to this branch.

## How the rebrand works

The repo contains the original APK zip (`com.dazzleo.indianbnserial
v1.0.1_antisplit.zip`) plus everything needed to rebuild it rebranded:

```
.github/workflows/build-apk.yml   CI workflow: decode → patch → build → sign → release
patcher/rebrand.py                Rebrand patcher (no dependencies, stdlib only)
brand/apk/                        New launcher icons, splash, in-app logos
brand/master_icon.png             Master icon artwork
signing/towfik-release.jks        Signing keystore (fresh key created for this app)
```

The patcher:

1. **AndroidManifest.xml** – package → `com.towfikserial.platform`,
   label → `Towfik Serial Platform`, permission names renamed.
2. **Flutter engine (`lib/arm64-v8a/libapp.so`)** – rewrites the brand strings
   inside the Dart AOT snapshot ("Indian Bangla Serial" → "Towfik Serial
   Platform", footer, share hashtags, share text, disclaimer). Length tables
   in the snapshot's alloc/fill sections are kept byte-identical, so the
   binary stays valid.
3. **Pairip Play-license check** – neutralized (it would otherwise block a
   re-signed build with a "Get this app from Play" screen).
4. **Images** – replaces launcher mipmaps (all densities), splash and both
   in-app logos.

Kept intentionally unchanged (functional identifiers): Firebase/AdMob config,
backend API URLs, the PiP platform channel, and the support email addresses.

## Local build (optional)

```bash
java -jar apktool.jar d -f -o decoded "com.dazzleo.indianbnserial v1.0.1_antisplit.zip"
python3 patcher/rebrand.py decoded brand/apk
java -jar apktool.jar b decoded -o unsigned.apk
zipalign -p -f 4 unsigned.apk aligned.apk
apksigner sign --ks signing/towfik-release.jks \
  --ks-pass pass:towfik2025key --key-pass pass:towfik2025key \
  --ks-key-alias towfik --out TowfikSerialPlatform-v1.0.1.apk aligned.apk
```
