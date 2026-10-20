# Gallery (Android)

A gallery app that shows the photos and videos already on your device.

- Reads media from `MediaStore` (real device photos and videos — nothing bundled).
- Asks for media permission on launch (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` on
  Android 13+, `READ_EXTERNAL_STORAGE` below that).
- Photos tab, Albums tab (grouped by folder) and Favourites tab.
- Full-screen viewer with swipe, a filmstrip, Share, Favourite, Delete and Info.

## Build the APK
Pushing to `main` runs **Build APK** in GitHub Actions. Open the run and download
the `gallery-debug-apk` artifact, which contains `app-debug.apk`.
