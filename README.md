# Restore stock notification icon

A resource patch for Morphe / ReVanced-style patch trees. After the **Custom branding** patch has
run, it puts YouTube's original small status-bar notification icon back.

> **Status: untested.** This was written without access to the Custom branding source, so the
> notification drawables are discovered by name hints instead of hardcoded names. Verify step 4
> below before relying on it.

## How it works

Two resource patches live in `RestoreStockNotificationIconPatch.kt`:

1. `stockNotificationIconSnapshotPatch` (hidden helper) records, before branding runs:
   - every `res/drawable*` file whose name matches `NOTIFICATION_DRAWABLE_HINTS`
   - the manifest `<meta-data>` value for `com.google.firebase.messaging.default_notification_icon`
2. `restoreStockNotificationIconPatch` ("Restore stock notification icon") depends on Custom
   branding, so it runs after it. It writes the original bytes back wherever branding changed or
   removed a captured file (removing same-name siblings such as `foo.png` vs `foo.xml` to avoid
   duplicate-resource errors) and resets the manifest reference.

If nothing needed restoring it logs a warning instead of failing silently.

## Integration

1. Copy `patches/src/main/kotlin/.../RestoreStockNotificationIconPatch.kt` into your patches tree
   and fix the `package` line to match its folder.
2. Fix the `customBrandingPatch` import. Find the real symbol with:
   `grep -rn "Custom branding" patches/src/main/kotlin`
3. Add `stockNotificationIconSnapshotPatch` to Custom branding's `dependsOn(...)`. This one-line
   edit guarantees the order snapshot -> Custom branding -> restore.
4. Check the names: run `apktool d` on the stock APK and list
   `res/drawable*/*notif*`. Adjust `NOTIFICATION_DRAWABLE_HINTS` (and `NOTIFICATION_META_KEYS`)
   to what you actually see.
5. Build and apply:
   - Morphe patches template: `./gradlew buildAndroid` produces `patches/build/libs/patches-*.mpp`
     (needs a GitHub PAT for GitHub Packages, see the Morphe docs).
   - Apply with Morphe Desktop:
     `java -jar morphe-desktop-*-all.jar patch -p patches-*.mpp youtube.apk`
   - ReVanced tree: replace `app.morphe.patcher` with `app.revanced.patcher` in the imports and
     build with that tree's normal Gradle task.

## Limits

- Resource-only. If your Custom branding build picks the notification icon at runtime (a bytecode
  hook plus an in-app "Notification icon" setting), this patch logs a warning and changes nothing.
  Use that setting, or change its default in the extension code.
- Selecting this patch also enables Custom branding, because of the dependency.
- Re-patching with Custom branding's "App icon" set to "Original" also restores the notification
  icon, but it reverts the launcher icon too.

## License

No license file is included yet. Morphe patches are GPLv3 with extra terms; pick a license that
is compatible with the tree you integrate this into.
