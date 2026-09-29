#!/usr/bin/env python3
"""
Rebrand patcher for the Indian Bangla Serial Flutter APK (decoded via apktool).

Renames the app to "Towfik Serial Platform":
  1. AndroidManifest.xml     -> package / label / permission names
  2. lib/arm64-v8a/libapp.so -> Flutter AOT string-pool brand strings
  3. Copies new launcher icons, splash and in-app logo images

The Flutter part is the delicate one: Dart AOT strings live in a serialized
object pool.  Each string is stored as
    [LEB128(length << 1 | is_two_byte)][raw bytes]
in the *fill* section, and the same encoded length appears again in the
*alloc* section.  Both regions must stay byte-identical in size, so the
replacement set below is balanced to a net byte delta of zero: the long
disclaimer text absorbs the remainder with invisible trailing spaces.
"""

import os
import re
import shutil
import sys

APP_NAME = "Towfik Serial Platform"
OLD_PACKAGE = "com.dazzleo.indianbnserial"
NEW_PACKAGE = "com.towfikserial.platform"
OLD_LABEL = "IndianBanglaTVSerial"

# ---------------------------------------------------------------- helpers

def read_leb(data, p):
    """Dart WriteUnsigned encoding: 7-bit groups, LAST byte carries +0x80."""
    v = 0
    shift = 0
    while True:
        b = data[p]
        p += 1
        if b >= 0x80:
            v |= (b - 0x80) << shift
            return v, p
        v |= b << shift
        shift += 7
        if shift > 63:
            raise ValueError("LEB too long")


def write_leb(v):
    out = bytearray()
    while True:
        b = v & 0x7F
        v >>= 7
        if v:
            out.append(b)
        else:
            out.append(b + 0x80)
            return bytes(out)


def enc(length, two_byte):
    return (length << 1) | (1 if two_byte else 0)


def weight(text):
    """Byte width per char: 2 for two-byte (UTF-16) strings, else 1."""
    return 2 if any(ord(c) > 0xFF for c in text) else 1

# ------------------------------------------------------- string patch set

_DISCLAIMER_OLD = (
    "All videos displayed in this app are embedded from publicly available "
    "third-party video sharing platforms. Indian Bangla Serial does not host, "
    "upload, or store any video files on its servers."
)
_DISCLAIMER_NEW = (
    "All videos displayed in this app are embedded from publicly available "
    "third-party video sharing platforms. Towfik Serial Platform does not "
    "host, upload, or store any video files on its servers."
)

_FIXED_PATCHES = [
    # (old, new)
    ("Indian Bangla Serial",
     "Towfik Serial Platform"),                                    # +2
    ("New episodes & announcements from Indian Bangla Serial",
     "New episodes & announcements from Towfik Serial Platform"),  # +2
    ("© 2025 Dazzleo · com.dazzleo.indianbnserial",
     "Towfik Serial Platform"),                                    # -21
    ("\n\n#IndianBanglaSeria #BanglaSerial #",
     "\n\n#TowfikSerial #BanglaSerial #"),                         # -6
    ("📲 Download the Indian Bangla Serial App:\n",
     "📲 Download Towfik Serial Platform App:\n"),                 # -3 chars x2 bytes
]

def _build_patch_set():
    patches = {}
    old_total = 0
    new_total = 0
    for old, new in _FIXED_PATCHES:
        patches[old] = new
        old_total += len(old) * weight(old)
        new_total += len(new) * weight(new)
    old_total += len(_DISCLAIMER_OLD) * weight(_DISCLAIMER_OLD)
    need = old_total - new_total - len(_DISCLAIMER_NEW)  # padding spaces to add
    if need < 0:
        raise RuntimeError("patch set overflows by %d bytes" % need)
    patches[_DISCLAIMER_OLD] = _DISCLAIMER_NEW + " " * need
    # final sanity: the whole set must be byte-neutral
    total = sum((len(n) - len(o)) * weight(o) for o, n in patches.items())
    if total != 0:
        raise RuntimeError("byte delta is %d, must be 0" % total)
    return patches

PATCH_STRINGS = _build_patch_set()

# functional identifiers that must NOT be touched even if they look branded
KEEP_EXACT = set()

def is_keep(text):
    return (text.startswith("com.dazzleo.indianbnserial")          # channel/pkg
            or text.startswith("https://play.google.com/store")    # store link
            or text.startswith("package:indianbnserial/")          # dart paths
            or text.startswith("https://scopebd.com")              # backend API
            or "@gmail.com" in text)                               # contact emails

# ------------------------------------------------------------ libapp.so

def patch_libapp(path):
    data = bytearray(open(path, "rb").read())
    anchor = b"New episodes & announcements from Indian Bangla Serial"
    a = data.find(anchor)
    if a < 0:
        raise RuntimeError("anchor string not found")
    a -= 1                                    # its single-byte LEB (0xEC)
    v, _ = read_leb(data, a)
    if v != enc(len(anchor), False):
        raise RuntimeError("anchor LEB mismatch")

    # --- walk the fill section, collecting every string entry
    entries = []                              # (leb_pos, enc, text, two)
    p = a
    n = len(data)
    while p < n:
        try:
            start = p
            e, q = read_leb(data, p)
            l = e >> 1
            two = e & 1
            nb = l * 2 if two else l
            if l > 500000 or q + nb > n:
                break
            raw = bytes(data[q:q + nb])
            text = raw.decode("utf-16-le", "replace") if two else raw.decode("latin1")
            entries.append((start, e, text, two))
            p = q + nb
        except Exception:
            break
    print("  fill section: %d strings [%s..%s)" % (len(entries), hex(a), hex(p)))

    # --- every patch target must appear exactly once
    idx_by_text = {}
    for i, (_, _, text, _) in enumerate(entries):
        if text in PATCH_STRINGS:
            if text in idx_by_text:
                raise RuntimeError("duplicate target: %r" % text[:40])
            idx_by_text[text] = i
    for old in PATCH_STRINGS:
        if old not in idx_by_text:
            raise RuntimeError("target missing in fill section: %r" % old[:50])

    # --- rebuild the fill section (size must stay identical)
    out = bytearray()
    new_lens = []                             # (entry_index, new_len, two)
    for i, (_, e, text, two) in enumerate(entries):
        if text in PATCH_STRINGS:
            new_text = PATCH_STRINGS[text]
            new_lens.append((i, len(new_text), two))
            payload = new_text.encode("utf-16-le") if two else new_text.encode("latin1")
        else:
            payload = text.encode("utf-16-le") if two else text.encode("latin1")
        out += write_leb(enc(len(payload) // (2 if two else 1), two))
        out += payload
    old_size = p - a
    if len(out) != old_size:
        raise RuntimeError("fill size changed %d -> %d" % (old_size, len(out)))
    data[a:p] = out
    print("  fill section rebuilt, size preserved (%d bytes)" % old_size)

    # --- locate the alloc section (same encoded lengths, same order)
    probe = [entries[i][1] for i in range(60)]
    alloc = None
    search_from = 0x340                       # .rodata start
    first_leb = write_leb(probe[0])
    while True:
        idx = data.find(first_leb, search_from, p)
        if idx < 0:
            break
        try:
            q, ok = idx, True
            for e in probe:
                v, q = read_leb(data, q)
                if v != e:
                    ok = False
                    break
        except Exception:
            ok = False
        if ok:
            alloc = idx
            break
        search_from = idx + 1
    if alloc is None:
        raise RuntimeError("alloc section not found")
    print("  alloc section found at %s" % hex(alloc))

    # --- walk the alloc section in order, patch the changed lengths
    q = alloc
    targets = dict((i, (nl, two)) for i, nl, two in new_lens)
    for i in range(len(entries)):
        v, q2 = read_leb(data, q)
        if i in targets:
            if v != entries[i][1]:
                raise RuntimeError("alloc desync at entry %d" % i)
            nl, two = targets[i]
            old_leb, new_leb = write_leb(v), write_leb(enc(nl, two))
            if len(old_leb) != len(new_leb):
                raise RuntimeError("LEB width change at entry %d" % i)
            data[q:q2] = new_leb
        q = q2
    print("  alloc section patched (%d lengths)" % len(new_lens))

    open(path, "wb").write(bytes(data))

    # --- report leftovers (expected: emails, package id, store link)
    blob = open(path, "rb").read()
    for pat in (b"Dazzleo", b"Indian Bangla Serial",
                "Indian Bangla Serial".encode("utf-16-le")):
        for m in re.finditer(re.escape(pat), blob):
            print("  note: leftover %r at %s" % (pat[:20], hex(m.start())))

# ------------------------------------------------------------- manifest

def patch_manifest(root):
    path = os.path.join(root, "AndroidManifest.xml")
    s = open(path, "r", encoding="utf-8").read()
    orig = s
    s = s.replace('package="%s"' % OLD_PACKAGE, 'package="%s"' % NEW_PACKAGE)
    s = s.replace('android:label="%s"' % OLD_LABEL, 'android:label="%s"' % APP_NAME)
    s = s.replace('%s.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION' % OLD_PACKAGE,
                  '%s.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION' % NEW_PACKAGE)
    if s == orig:
        raise RuntimeError("manifest patterns not found")
    open(path, "w", encoding="utf-8").write(s)
    print("  manifest: package=%s label=%r" % (NEW_PACKAGE, APP_NAME))

# ------------------------------------------------------------- pairip

def neutralize_pairip(root):
    """Pairip's license check blocks re-signed builds ('Get this app from
    Play').  The only hook is LicenseClient.checkLicense() inside
    com.pairip.application.Application.attachBaseContext - no-op it."""
    path = os.path.join(root, "smali", "com", "pairip", "application", "Application.smali")
    if not os.path.exists(path):
        print("  pairip: not present, skipped")
        return
    s = open(path, "r", encoding="utf-8").read()
    if "checkLicense" not in s:
        print("  pairip: already neutralized")
        return
    s = s.replace(
        "    invoke-static {p1}, Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V\n",
        "")
    if "checkLicense" in s:
        raise RuntimeError("failed to neutralize pairip license check")
    open(path, "w", encoding="utf-8").write(s)
    print("  pairip: license check neutralized")

# --------------------------------------------------------------- images

IMAGE_MAP = {
    "res/mipmap-mdpi/ic_launcher.png":       "res/mipmap-mdpi/ic_launcher.png",
    "res/mipmap-hdpi/ic_launcher.png":       "res/mipmap-hdpi/ic_launcher.png",
    "res/mipmap-xhdpi/ic_launcher.png":      "res/mipmap-xhdpi/ic_launcher.png",
    "res/mipmap-xxhdpi/ic_launcher.png":     "res/mipmap-xxhdpi/ic_launcher.png",
    "res/mipmap-xxxhdpi/ic_launcher.png":    "res/mipmap-xxxhdpi/ic_launcher.png",
    "res/drawable-xxhdpi/splash.png":        "res/drawable-xxhdpi/splash.png",
    "res/drawable/background.png":           "res/drawable/background.png",
    "assets/flutter_assets/assets/images/logo.png":             "flutter/images/logo.png",
    "assets/flutter_assets/assets/images/logo_transparent.png": "flutter/images/logo_transparent.png",
}

def copy_images(root, brand_dir):
    for dst_rel, src_rel in IMAGE_MAP.items():
        src = os.path.join(brand_dir, src_rel)
        dst = os.path.join(root, dst_rel)
        if not os.path.exists(src):
            raise RuntimeError("brand asset missing: %s" % src)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
    print("  images: %d assets replaced" % len(IMAGE_MAP))

# ----------------------------------------------------------------- main

def main():
    root = os.path.abspath(sys.argv[1])
    brand = os.path.abspath(sys.argv[2] if len(sys.argv) > 2 else
                            os.path.join(os.path.dirname(__file__), "..", "brand", "apk"))
    print("[1/3] Flutter strings (libapp.so)")
    patch_libapp(os.path.join(root, "lib", "arm64-v8a", "libapp.so"))
    print("[2/3] AndroidManifest.xml")
    patch_manifest(root)
    print("[2b/] Pairip license check")
    neutralize_pairip(root)
    print("[3/3] Branding images")
    copy_images(root, brand)
    print("Rebrand patch complete.")

if __name__ == "__main__":
    main()
