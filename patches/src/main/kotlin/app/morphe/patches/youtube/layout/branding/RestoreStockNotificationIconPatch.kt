package app.morphe.patches.youtube.layout.branding

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.InstallerType
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.bytecodePatch
// Note: GmsCoreSupportPatch.kt also declares an EXTENSION_CLASS. This is the branding one.
import app.morphe.patches.shared.layout.branding.EXTENSION_CLASS
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.AccessFlags

/**
 * The extension's `CustomBrandingPatch.getNotificationSmallIcon()`:
 * returns the resource id of the branded small notification icon, or 0 for "no override".
 */
internal object NotificationSmallIconExtensionFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "getNotificationSmallIcon",
    accessFlags = listOf(AccessFlags.PRIVATE, AccessFlags.STATIC),
    returnType = "I",
    parameters = listOf()
)

@Suppress("unused")
val restoreStockNotificationIconPatch = bytecodePatch(
    name = "Restore stock notification icon",
    description = "Keeps YouTube's original small status bar notification icon " +
            "instead of the icon chosen by Custom branding."
) {
    // Same availability as Custom branding: a mounted (root) install has no runtime hook to undo.
    availability { installer, _ ->
        when (installer) {
            InstallerType.MOUNT -> PatchAvailability.DISABLED
            else -> PatchAvailability.ENABLED
        }
    }

    // Custom branding must run first so its extension class and notification hooks exist.
    dependsOn(customBrandingPatch)

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        // With 0 ("no override") the extension's getSmallIcon(original) returns the app's own
        // icon id, and getColor(original) keeps the original tint instead of forcing transparent.
        NotificationSmallIconExtensionFingerprint.method.returnEarly(0)
    }
}
