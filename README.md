# Gallery (Android) — v3

A clean, visual-first gallery app that reads the photos and videos already on your device.

## Features
- **Photos tab** — filter chips (All / Years / Months / Days), sticky date headers,
  video duration badges, and **pinch to zoom the grid from 1 to 6 columns**.
- **Fullscreen viewer** — pure black, single tap hides the bars, swipe down to dismiss,
  inline video playback, pinch-to-zoom, filmstrip, share / edit / trash / tools.
- **Info sheet** — date, time, size, resolution + megapixels, format, path, and camera
  EXIF (make, model, ISO, shutter, aperture, focal length).
- **Albums tab** — Favourites / Recents / Trash cards, smart categories
  (Videos, Screenshots, Camera, Downloads, GIFs) and 2-column folder cards.
- **Search tab** — live search by name or album.
- **Settings tab** — theme (System / Light / Dark / AMOLED black), default grid size,
  hidden folder, about.
- **Trash** — moves items to the system bin (Android 11+) instead of deleting outright.
- **Hidden folder** — biometric/PIN locked list of items you mark as hidden.
- **Editor (basic)** — rotate, flip and colour filters, saved as a new copy.
- **Multi-select** — long-press to select, then share / trash / hide / favourite.

All media is read from `MediaStore` on the device. Nothing is uploaded anywhere.

## Permissions
Requested on first launch: `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (Android 13+),
`READ_EXTERNAL_STORAGE` (below), plus `USE_BIOMETRIC` and `SET_WALLPAPER`.

## Build
Pushing to `main` runs **Build APK** in GitHub Actions. Download the
`gallery-debug-apk` artifact, which contains `app-debug.apk`.
