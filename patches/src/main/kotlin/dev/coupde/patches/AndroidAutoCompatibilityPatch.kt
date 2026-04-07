package dev.coupde.patches

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val androidAutoCompatibilityPatch = resourcePatch(
    name = "Android Auto compatibility",
    description = "Makes any app compatible with Android Auto by injecting car app metadata, " +
        "automotive app descriptor, and required manifest entries. " +
        "Ideal for media apps like YouTube ReVanced, Spotify, etc.",
) {
    apply {
        // Step 1: Create res/xml/automotive_app_desc.xml
        val xmlDir = get("res/xml", copy = false)
        xmlDir.mkdirs()
        File(xmlDir, "automotive_app_desc.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="media" />
    <uses name="notification" />
</automotiveApp>""",
        )

        // Step 2: Modify AndroidManifest.xml
        document("AndroidManifest.xml").use { doc ->
            val document = doc.file
            val manifest = document.getElementsByTagName("manifest").item(0) as Element
            val applicationNodes = document.getElementsByTagName("application")

            if (applicationNodes.length == 0) {
                throw Exception("No <application> tag found in AndroidManifest.xml")
            }

            val application = applicationNodes.item(0) as Element

            // Add car app metadata
            addMetadataIfMissing(
                document, application,
                "com.google.android.gms.car.application",
                resource = "@xml/automotive_app_desc",
            )

            // Add CAR_DOCK / CAR_MODE categories to launcher activity
            addCarLauncherCategory(document, application)

            // Add automotive uses-feature (required=false)
            addUsesFeatureIfMissing(document, manifest, "android.hardware.type.automotive", required = false)

            // Add FOREGROUND_SERVICE permission
            addPermissionIfMissing(document, manifest, "android.permission.FOREGROUND_SERVICE")
        }
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

private fun addCategoryIfMissing(document: org.w3c.dom.Document, intentFilter: Element, categoryName: String) {
    val categories = intentFilter.getElementsByTagName("category")
    for (i in 0 until categories.length) {
        val cat = categories.item(i) as? Element ?: continue
        if (cat.getAttribute("android:name") == categoryName) return
    }
    val category = document.createElement("category")
    category.setAttribute("android:name", categoryName)
    intentFilter.appendChild(category)
}

private fun addUsesFeatureIfMissing(document: org.w3c.dom.Document, manifest: Element, featureName: String, required: Boolean) {
    val features = manifest.getElementsByTagName("uses-feature")
    for (i in 0 until features.length) {
        val feature = features.item(i) as? Element ?: continue
        if (feature.getAttribute("android:name") == featureName) return
    }
    val usesFeature = document.createElement("uses-feature")
    usesFeature.setAttribute("android:name", featureName)
    usesFeature.setAttribute("android:required", required.toString())
    manifest.appendChild(usesFeature)
}

private fun addPermissionIfMissing(document: org.w3c.dom.Document, manifest: Element, permission: String) {
    val permissions = manifest.getElementsByTagName("uses-permission")
    for (i in 0 until permissions.length) {
        val perm = permissions.item(i) as? Element ?: continue
        if (perm.getAttribute("android:name") == permission) return
    }
    val usesPerm = document.createElement("uses-permission")
    usesPerm.setAttribute("android:name", permission)
    manifest.appendChild(usesPerm)
}
