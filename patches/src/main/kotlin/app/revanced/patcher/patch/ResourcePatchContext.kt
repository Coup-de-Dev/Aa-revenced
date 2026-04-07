package app.revanced.patcher.patch

import app.revanced.patcher.util.Document
import java.io.File

class ResourcePatchContext internal constructor() {
    fun document(path: String): Document = throw UnsupportedOperationException("stub")
    operator fun get(path: String, copy: Boolean = true): File = throw UnsupportedOperationException("stub")
    fun delete(name: String): Boolean = throw UnsupportedOperationException("stub")
}
