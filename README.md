# Restore stock notification icon

A bytecode patch for the [Morphe patches](https://github.com/MorpheApp/morphe-patches) tree. It keeps
YouTube's original small status-bar notification icon when the **Custom branding** patch is applied.

> **Status: not compiled.** Written against the Morphe patches source at commit `f3acc10`; every
> symbol it uses was checked against that source by reading it, but no Gradle build was run
> (the build dependencies are hosted on GitHub Packages and were not reachable from the environment
> this was written in). Build it before relying on it.

## Why a bytecode patch

Custom branding does not overwrite the stock notification drawable on normal (non-root) installs.
It hooks the notification builder and routes the small icon through the extension method
`CustomBrandingPatch.getSmallIcon(int)`, which picks an icon at runtime from the in-app
"Notification icon" setting. That setting defaults to "Match app icon", so the branded icon wins.
The stock drawable (`ic_stat_yt_notification_logo`) is still in the APK, untouched.

So there is no resource to restore. The fix is to turn the override off.

## What the patch does

It makes the extension's private `getNotificationSmallIcon()` return `0` ("no override"). Then:

- `getSmallIcon(original)` returns the app's own icon id, and
- `getColor(original)` returns the original tint instead of forcing transparent.

Result: the stock icon and tint, while the launcher icon and app name stay branded.

## Integration

1. Copy `patches/src/main/kotlin/app/morphe/patches/youtube/layout/branding/RestoreStockNotificationIconPatch.kt`
   into the same folder of your `morphe-patches` checkout. It shares the package of
   `customBrandingPatch`, so no import for that is needed.
2. Build: `./gradlew buildAndroid`. The `.mpp` appears in `patches/build/libs/patches-*.mpp`
   (needs the GitHub Packages credentials described in the Morphe docs).
3. Apply it with Morphe Desktop or add the bundle as a source in Morphe Manager, and enable
   **Restore stock notification icon** next to **Custom branding**.

Selecting this patch also selects Custom branding, because of the dependency.

## Limits

- The in-app **Notification icon** setting stops having an effect when this patch is applied.
- Mounted (root) installs: Custom branding replaces the drawable file itself while patching,
  so this patch is disabled there. Set Custom branding's **App icon** option to **Original** instead.
- YouTube only. YouTube Music uses the same base patch but is not covered.

## No patch needed?

The extension has a `NotificationIconTheme.ORIGINAL` value, so on a normal install you can also
open the Custom branding settings in the patched app and set **Notification icon** to **Original**.
This patch makes that the baked-in behavior.

## License

No license file is included yet. Morphe patches are GPLv3 with extra terms (attribution and a
restriction on using the "Morphe" name for derivative works), so pick a compatible license.
