@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package app.revanced.patcher.patch

typealias PackageName = String
typealias VersionName = String
typealias Package = Pair<PackageName, Set<VersionName>?>

open class Patch internal constructor(
    val name: String?,
    val description: String?,
    val use: Boolean,
)

open class ResourcePatchBuilder internal constructor() {
    private var applyBlock: (ResourcePatchContext.() -> Unit)? = null

    fun apply(block: ResourcePatchContext.() -> Unit) {
        applyBlock = block
    }

    operator fun String.invoke(vararg versions: VersionName) = invoke(versions.toSet())
    private operator fun String.invoke(versions: Set<VersionName>? = null): Package = this to versions

    fun compatibleWith(vararg packages: Package) {}
    fun compatibleWith(vararg packages: String) {}
    fun dependsOn(vararg patches: Patch) {}

    internal fun build(name: String?, description: String?, use: Boolean) = Patch(name, description, use)
}

class PatchException(errorMessage: String?, cause: Throwable?) : Exception(errorMessage, cause) {
    constructor(errorMessage: String) : this(errorMessage, null)
    constructor(cause: Throwable) : this(cause.message, cause)
}

fun resourcePatch(
    name: String? = null,
    description: String? = null,
    use: Boolean = true,
    block: ResourcePatchBuilder.() -> Unit,
) = ResourcePatchBuilder().also(block).build(name, description, use)
