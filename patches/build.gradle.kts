plugins {
    alias(libs.plugins.kotlin)
}

group = "dev.coupde"
version = "1.0.0"

val r8: Configuration by configurations.creating

dependencies {
    // Stubs are compiled from source (in app/revanced/patcher/), no external dep needed
    r8("com.android.tools:r8:8.3.37")
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    archiveBaseName.set("aa-revanced-patches")

    // Exclude patcher stubs from the JAR — only our patch class should be included
    exclude("app/revanced/**")

    manifest {
        attributes(
            "Patch-Classes" to "dev.coupde.patches.AndroidAutoCompatibilityPatchKt",
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

        // Provide the stubs JAR as classpath so d8 can resolve parent classes
        // Build a stubs-only JAR for d8 classpath
        val stubsJar = layout.buildDirectory.file("stubs/patcher-stubs.jar").get().asFile
        stubsJar.parentFile.mkdirs()

        // Create stubs JAR from compiled classes (only app/revanced/** classes)
        val classesDir = layout.buildDirectory.dir("classes/kotlin/main").get().asFile
        ant.withGroovyBuilder {
            "jar"("destfile" to stubsJar.absolutePath, "basedir" to classesDir.absolutePath) {
                "include"("name" to "app/revanced/**")
            }
        }

        val argsList = mutableListOf(
            "--release",
            "--min-api", "26",
            "--output", outputDir.absolutePath,
            "--classpath", stubsJar.absolutePath,
            jarFile.get().asFile.absolutePath,
        )

        args(argsList)
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

    // Include resource files from the original JAR
    from(zipTree(tasks.jar.get().archiveFile)) {
        include("META-INF/**")
        include("**/*.xml")
        exclude("**/*.class")
    }

    manifest {
        attributes(
            "Patch-Classes" to "dev.coupde.patches.AndroidAutoCompatibilityPatchKt",
        )
    }
}
