package dev.hyzor.patches.duolingo.misc.icon

import app.morphe.patcher.patch.resourcePatch
import dev.hyzor.patches.duolingo.shared.Constants.DUOLINGO
import dev.hyzor.patches.duolingo.shared.integrity.disableLoginIntegrityPatch
import org.w3c.dom.Element

/**
 * Duolingo ships one `activity-alias` per selectable app icon (seasonal icons, streak society,
 * and the `LoginActivity`/`IconActivity` aliases it switches between). The aliases that are
 * shipped disabled are the ones the app can enable later, so removing them leaves only the
 * default icon and stops Duolingo from changing it.
 */
@Suppress("unused")
val disableDynamicIconPatch = resourcePatch(
    name = "Disable dynamic app icon",
    description = "Prevents Duolingo from changing the app icon. Only the default icon will be available.",
    default = false,
) {
    compatibleWith(DUOLINGO)

    dependsOn(disableLoginIntegrityPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val aliases = document.getElementsByTagName("activity-alias")

            val toRemove = buildList {
                for (index in 0 until aliases.length) {
                    val element = aliases.item(index) as? Element ?: continue
                    if (element.getAttribute("android:enabled") == "false" &&
                        element.getAttribute("android:targetActivity") == "com.duolingo.splash.LaunchActivity"
                    ) {
                        add(element)
                    }
                }
            }

            toRemove.forEach { it.parentNode.removeChild(it) }
        }
    }
}
