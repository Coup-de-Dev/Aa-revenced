plugins {
    alias(libs.plugins.kotlin)
}

group = "dev.coupde"
version = "1.0.0"

val r8: Configuration by configurations.creating

dependencies {
    implementation(libs.revanced.patcher)
    r8("com.android.tools:r8:8.3.37")
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveBaseName.set("aa-revanced-patches")

    manifest {
        attributes(
            "Patch-Classes" to "dev.coupde.patches.AndroidAutoCompatibilityPatch",
        )
    }
}

// Convert JVM bytecode to DEX format for ReVanced Manager (Android)
tasks.register<JavaExec>("dex") {
    description = "Compile JAR to DEX format for ReVanced Manager"
    dependsOn(tasks.jar)

    val jarFile = tasks.jar.get().archiveFile
    val dexDir = layout.buildDirectory.dir("dex")

    inputs.file(jarFile)
    outputs.dir(dexDir)

    classpath = r8
    mainClass.set("com.android.tools.r8.D8")

    doFirst {
        val outputDir = dexDir.get().asFile
        outputDir.mkdirs()

        args(
            "--release",
            "--min-api", "26",
            "--output", outputDir.absolutePath,
            jarFile.get().asFile.absolutePath,
        )
    }
}

// Repackage: put DEX files + resources back into a JAR/RVP
tasks.register<Jar>("rvp") {
    description = "Create .rvp (DEX-based JAR) for ReVanced Manager"
    dependsOn("dex")

    archiveBaseName.set("aa-revanced-patches")
    archiveExtension.set("rvp")

    val dexDir = layout.buildDirectory.dir("dex")

    from(dexDir) {
        include("*.dex")
    }

    // Include the manifest and resource files from the original JAR
    from(zipTree(tasks.jar.get().archiveFile)) {
        include("META-INF/**")
        include("android-auto/**")
        include("**/*.xml")
        exclude("**/*.class")
    }

    manifest {
        attributes(
            "Patch-Classes" to "dev.coupde.patches.AndroidAutoCompatibilityPatch",
        )
    }
}
