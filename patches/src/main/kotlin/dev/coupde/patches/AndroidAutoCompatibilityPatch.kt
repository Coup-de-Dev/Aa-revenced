package dev.coupde.patches

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import org.w3c.dom.Element
import java.io.File

@Patch(
    name = "Android Auto compatibility",
    description = "Makes any app compatible with Android Auto by injecting car app metadata, " +
        "automotive app descriptor, and required manifest entries. " +
        "Ideal for media apps like YouTube ReVanced, Spotify, etc.",
    use = true,
)
@Suppress("unused")
object AndroidAutoCompatibilityPatch : ResourcePatch() {

    private const val AUTOMOTIVE_APP_DESC_XML = """<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="media" />
    <uses name="notification" />
</automotiveApp>"""

    override fun execute(context: ResourceContext) {
        // Step 1: Create res/xml/automotive_app_desc.xml in the APK working directory
        val xmlDir = context["res/xml"]
        xmlDir.mkdirs()
        File(xmlDir, "automotive_app_desc.xml").writeText(AUTOMOTIVE_APP_DESC_XML)

        // Step 2: Modify AndroidManifest.xml to add Android Auto support
        context.xmlEditor["AndroidManifest.xml"].use { editor ->
            val document = editor.file
            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            val applicationNodes = document.getElementsByTagName("application")

            if (applicationNodes.length == 0) {
                throw Exception("No <application> tag found in AndroidManifest.xml")
            }

            val application = applicationNodes.item(0) as Element

            // 2a. Add car app metadata to <application>
            addMetadataIfMissing(
                document,
                application,
                "com.google.android.gms.car.application",
                resource = "@xml/automotive_app_desc",
            )

            // 2b. Add CAR_DOCK and CAR_MODE categories to the launcher activity
            addCarLauncherCategory(document, application)

            // 2c. Add automotive uses-feature (required=false so it still installs on phones)
            addUsesFeatureIfMissing(
                document,
                manifest,
                "android.hardware.type.automotive",
                required = false,
            )

            // 2d. Add FOREGROUND_SERVICE permission (needed for media playback in Auto)
            addPermissionIfMissing(document, manifest, "android.permission.FOREGROUND_SERVICE")
        }
    }

    private fun addMetadataIfMissing(
        document: org.w3c.dom.Document,
        application: Element,
        name: String,
        resource: String? = null,
        value: String? = null,
    ) {
        val metadataNodes = application.getElementsByTagName("meta-data")
        for (i in 0 until metadataNodes.length) {
            val node = metadataNodes.item(i) as? Element ?: continue
            if (node.getAttribute("android:name") == name) {
                resource?.let { node.setAttribute("android:resource", it) }
                value?.let { node.setAttribute("android:value", it) }
                return
            }
        }

        val metaData = document.createElement("meta-data")
        metaData.setAttribute("android:name", name)
        resource?.let { metaData.setAttribute("android:resource", it) }
        value?.let { metaData.setAttribute("android:value", it) }
        application.appendChild(metaData)
    }

    private fun addCarLauncherCategory(document: org.w3c.dom.Document, application: Element) {
        val activities = application.getElementsByTagName("activity")

        for (i in 0 until activities.length) {
            val activity = activities.item(i) as? Element ?: continue
            val intentFilters = activity.getElementsByTagName("intent-filter")

            for (j in 0 until intentFilters.length) {
                val intentFilter = intentFilters.item(j) as? Element ?: continue
                val categories = intentFilter.getElementsByTagName("category")

                var isLauncher = false
                for (k in 0 until categories.length) {
                    val cat = categories.item(k) as? Element ?: continue
                    if (cat.getAttribute("android:name") == "android.intent.category.LAUNCHER") {
                        isLauncher = true
                        break
                    }
                }

                if (isLauncher) {
                    addCategoryIfMissing(document, intentFilter, "android.intent.category.CAR_DOCK")
                    addCategoryIfMissing(document, intentFilter, "android.intent.category.CAR_MODE")
                    return
                }
            }
        }
    }

    private fun addCategoryIfMissing(
        document: org.w3c.dom.Document,
        intentFilter: Element,
        categoryName: String,
    ) {
        val categories = intentFilter.getElementsByTagName("category")
        for (i in 0 until categories.length) {
            val cat = categories.item(i) as? Element ?: continue
            if (cat.getAttribute("android:name") == categoryName) {
                return
            }
        }

        val category = document.createElement("category")
        category.setAttribute("android:name", categoryName)
        intentFilter.appendChild(category)
    }

    private fun addUsesFeatureIfMissing(
        document: org.w3c.dom.Document,
        manifest: Element,
        featureName: String,
        required: Boolean,
    ) {
        val features = manifest.getElementsByTagName("uses-feature")
        for (i in 0 until features.length) {
            val feature = features.item(i) as? Element ?: continue
            if (feature.getAttribute("android:name") == featureName) {
                return
            }
        }

        val usesFeature = document.createElement("uses-feature")
        usesFeature.setAttribute("android:name", featureName)
        usesFeature.setAttribute("android:required", required.toString())
        manifest.appendChild(usesFeature)
    }

    private fun addPermissionIfMissing(
        document: org.w3c.dom.Document,
        manifest: Element,
        permission: String,
    ) {
        val permissions = manifest.getElementsByTagName("uses-permission")
        for (i in 0 until permissions.length) {
            val perm = permissions.item(i) as? Element ?: continue
            if (perm.getAttribute("android:name") == permission) {
                return
            }
        }

        val usesPerm = document.createElement("uses-permission")
        usesPerm.setAttribute("android:name", permission)
        manifest.appendChild(usesPerm)
    }
}
