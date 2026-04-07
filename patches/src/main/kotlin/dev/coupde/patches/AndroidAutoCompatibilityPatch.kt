package dev.coupde.patches

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.io.File

@Suppress("unused")
val androidAutoCompatibilityPatch = resourcePatch(
    name = "Android Auto compatibility",
    description = "Makes any app compatible with Android Auto (video in parked mode + media). " +
        "Requires Android 16+ and Android Auto v16.3+.",
) {
    apply {
        // Step 1: Create res/xml/automotive_app_desc.xml with ALL capabilities
        val xmlDir = get("res/xml", copy = false)
        xmlDir.mkdirs()
        File(xmlDir, "automotive_app_desc.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<automotiveApp>
    <uses name="media" />
    <uses name="video" />
    <uses name="notification" />
    <uses name="template" />
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
            val packageName = manifest.getAttribute("package")

            // === KEY: Set appCategory="video" so Android Auto treats this as a video app ===
            application.setAttribute("android:appCategory", "video")

            // Add car app metadata pointing to our descriptor
            addMetadataIfMissing(
                document, application,
                "com.google.android.gms.car.application",
                resource = "@xml/automotive_app_desc",
            )

            // Add metadata to allow background audio while driving (for video apps)
            addUsesFeatureIfMissing(
                document, manifest,
                "com.android.car.background_audio_while_driving",
                required = false,
            )

            // Add FOREGROUND_SERVICE permissions
            addPermissionIfMissing(document, manifest, "android.permission.FOREGROUND_SERVICE")
            addPermissionIfMissing(document, manifest, "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK")

            // === Ensure a MediaBrowserService is declared ===
            // Check if one already exists in the app
            val existingMBS = findExistingMediaBrowserService(application)
            if (existingMBS == null) {
                // YouTube 20.x+ has an internal media service, try to declare one
                // pointing to known YouTube service classes
                addMediaBrowserServiceDeclaration(document, application, packageName)
            }

            // Add CAR_DOCK/CAR_MODE categories to launcher (legacy but doesn't hurt)
            addCarLauncherCategory(document, application)
        }
    }
}

/**
 * Finds an existing MediaBrowserService declaration in the manifest.
 */
private fun findExistingMediaBrowserService(application: Element): Element? {
    val services = application.getElementsByTagName("service")
    for (i in 0 until services.length) {
        val service = services.item(i) as? Element ?: continue
        val intentFilters = service.getElementsByTagName("intent-filter")
        for (j in 0 until intentFilters.length) {
            val actions = (intentFilters.item(j) as? Element)?.getElementsByTagName("action") ?: continue
            for (k in 0 until actions.length) {
                val actionName = (actions.item(k) as? Element)?.getAttribute("android:name")
                if (actionName == "android.media.browse.MediaBrowserService") {
                    return service
                }
            }
        }
    }
    return null
}

/**
 * Adds a MediaBrowserService intent-filter to an existing service or creates one.
 * For YouTube, we look for known media-related service classes.
 */
private fun addMediaBrowserServiceDeclaration(
    document: org.w3c.dom.Document,
    application: Element,
    packageName: String,
) {
    // Look for existing YouTube media-related services to add the intent-filter to
    val knownYouTubeServices = listOf(
        "com.google.android.apps.youtube.app.common.mediabrowser.YouTubeMediaBrowserService",
        "com.google.android.youtube.api.service.YouTubeMediaBrowserService",
        "com.google.android.apps.youtube.music.mediabrowser.MusicBrowserService",
    )

    val services = application.getElementsByTagName("service")
    for (i in 0 until services.length) {
        val service = services.item(i) as? Element ?: continue
        val serviceName = service.getAttribute("android:name")

        // Check if this is a known media service or contains "MediaBrowser" or "mediabrowser"
        if (knownYouTubeServices.any { serviceName == it } ||
            serviceName.contains("MediaBrowser", ignoreCase = true) ||
            serviceName.contains("mediabrowser", ignoreCase = true)
        ) {
            // Add the MediaBrowserService intent-filter to this existing service
            val intentFilter = document.createElement("intent-filter")
            val action = document.createElement("action")
            action.setAttribute("android:name", "android.media.browse.MediaBrowserService")
            intentFilter.appendChild(action)
            service.appendChild(intentFilter)
            service.setAttribute("android:exported", "true")
            return
        }
    }

    // If no existing media service found, create a new service declaration
    // pointing to the most common YouTube media browser service class name
    val service = document.createElement("service")
    service.setAttribute(
        "android:name",
        "com.google.android.apps.youtube.app.common.mediabrowser.YouTubeMediaBrowserService",
    )
    service.setAttribute("android:exported", "true")

    val intentFilter = document.createElement("intent-filter")
    val action = document.createElement("action")
    action.setAttribute("android:name", "android.media.browse.MediaBrowserService")
    intentFilter.appendChild(action)
    service.appendChild(intentFilter)

    application.appendChild(service)
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
