plugins {
    alias(libs.plugins.kotlin)
}

group = "dev.coupde"
version = "1.0.0"

dependencies {
    implementation(libs.revanced.patcher)
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
