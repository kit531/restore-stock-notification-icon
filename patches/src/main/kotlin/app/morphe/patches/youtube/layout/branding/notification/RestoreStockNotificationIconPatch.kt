package app.morphe.patches.youtube.layout.branding.notification

// ReVanced tree: replace `app.morphe.patcher` with `app.revanced.patcher` in the imports below.
import app.morphe.patcher.patch.resourcePatch
// TODO: import the REAL Custom branding patch symbol from your tree. Find it with:
//   grep -rn "Custom branding" patches/src/main/kotlin
import app.morphe.patches.youtube.layout.branding.customBrandingPatch
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.logging.Logger

private val logger = Logger.getLogger("RestoreStockNotificationIcon")

/**
 * File-name fragments that identify the app's small notification drawables.
 * Verify against your APK (see the integration notes) and adjust if needed.
 */
private val NOTIFICATION_DRAWABLE_HINTS = listOf(
    "notification_icon",
    "ic_notification",
    "ic_stat_",
    "stat_notify",
)

/** Manifest <meta-data> keys that point the system at the default small notification icon. */
private val NOTIFICATION_META_KEYS = setOf(
    "com.google.firebase.messaging.default_notification_icon",
)

/** What the stock APK looked like before any branding patch ran. */
private object StockSnapshot {
    /** Path relative to res/ -> original file bytes. */
    val files = linkedMapOf<String, ByteArray>()

    /** <meta-data android:name> -> original android:resource value. */
    val meta = linkedMapOf<String, String>()

    fun clear() {
        files.clear()
        meta.clear()
    }
}

private fun Document.metaDataElements(): List<Element> {
    val nodes = getElementsByTagName("meta-data")
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

/**
 * Hidden helper. Records the stock notification drawables and manifest references.
 * It must run BEFORE Custom branding, so add it to Custom branding's dependsOn(...).
 */
val stockNotificationIconSnapshotPatch = resourcePatch {
    execute {
        StockSnapshot.clear()

        val res = get("res")
        res.walkTopDown()
            .filter { it.isFile && it.parentFile.name.startsWith("drawable") }
            .filter { file -> NOTIFICATION_DRAWABLE_HINTS.any { file.name.contains(it, ignoreCase = true) } }
            .forEach { StockSnapshot.files[it.relativeTo(res).path] = it.readBytes() }

        document("AndroidManifest.xml").use { document ->
            document.metaDataElements()
                .filter { it.getAttribute("android:name") in NOTIFICATION_META_KEYS }
                .forEach { StockSnapshot.meta[it.getAttribute("android:name")] = it.getAttribute("android:resource") }
        }

        logger.info(
            "Snapshot: ${StockSnapshot.files.size} notification drawable(s), " +
                "${StockSnapshot.meta.size} manifest reference(s): ${StockSnapshot.files.keys}"
        )
    }
}

@Suppress("unused")
val restoreStockNotificationIconPatch = resourcePatch(
    name = "Restore stock notification icon",
    description = "Restores the original small status bar notification icon after Custom branding replaced it.",
) {
    compatibleWith("com.google.android.youtube")

    // Order is guaranteed: snapshot -> Custom branding -> this patch.
    dependsOn(stockNotificationIconSnapshotPatch, customBrandingPatch)

    execute {
        if (StockSnapshot.files.isEmpty() && StockSnapshot.meta.isEmpty()) {
            logger.warning(
                "Nothing was captured. NOTIFICATION_DRAWABLE_HINTS / NOTIFICATION_META_KEYS do not match " +
                    "this APK, or the snapshot patch is not a dependency of Custom branding."
            )
            return@execute
        }

        var restored = 0
        val res = get("res")

        // 1) Resource files: write the original bytes back wherever branding changed or removed them.
        StockSnapshot.files.forEach { (relativePath, original) ->
            val target = res.resolve(relativePath)
            if (target.exists() && target.readBytes().contentEquals(original)) return@forEach

            // Branding may have swapped e.g. foo.png for foo.xml. Remove same-name siblings so the
            // resource compiler does not see a duplicate resource.
            target.parentFile.mkdirs()
            target.parentFile.listFiles { f -> f != target && f.nameWithoutExtension == target.nameWithoutExtension }
                ?.forEach { it.delete() }

            target.writeBytes(original)
            restored++
        }

        // 2) Manifest: point the default notification icon meta-data back at the original resource.
        document("AndroidManifest.xml").use { document ->
            document.metaDataElements().forEach { element ->
                val original = StockSnapshot.meta[element.getAttribute("android:name")] ?: return@forEach
                if (element.getAttribute("android:resource") != original) {
                    element.setAttribute("android:resource", original)
                    restored++
                }
            }
        }

        if (restored == 0) {
            logger.warning(
                "Custom branding did not modify any captured file or manifest reference. " +
                    "It probably chooses the notification icon at runtime (bytecode/settings), " +
                    "which a resource patch cannot undo."
            )
        } else {
            logger.info("Restored $restored stock notification icon item(s).")
        }
    }
}
