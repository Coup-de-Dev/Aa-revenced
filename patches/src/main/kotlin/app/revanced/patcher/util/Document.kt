package app.revanced.patcher.util

import java.io.Closeable
import java.io.File
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class Document private constructor(
    val file: org.w3c.dom.Document,
) : Closeable {
    constructor(inputStream: InputStream) : this(
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream),
    )

    constructor(file: File) : this(file.inputStream())

    override fun close() {}
}
