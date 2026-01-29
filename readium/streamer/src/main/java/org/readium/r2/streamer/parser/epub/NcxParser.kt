/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.readium.r2.streamer.parser.epub

import org.readium.r2.shared.parser.xml.ElementNode
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.util.Href

internal object NcxParser {

    fun parse(document: ElementNode, filePath: String): Map<String, List<Link>> {
        val toc = (document.getFirst("navMap", Namespaces.NCX)
            ?: document.getFirst("navMap", ""))
            ?.let { parseNavMapElement(it, filePath) }?.let { Pair("toc", it) }
        val pageList = (document.getFirst("pageList", Namespaces.NCX)
            ?: document.getFirst("pageList", ""))
            ?.let { parsePageListElement(it, filePath) }?.let { Pair("page-list", it) }
        return listOfNotNull(toc, pageList).toMap()
    }

    private fun parseNavMapElement(element: ElementNode, filePath: String): List<Link> =
        (element.get("navPoint", Namespaces.NCX).takeIf { it.isNotEmpty() }
            ?: element.get("navPoint", ""))
            .mapNotNull { parseNavPointElement(it, filePath) }

    private fun parsePageListElement(element: ElementNode, filePath: String): List<Link> =
        (element.get("pageTarget", Namespaces.NCX).takeIf { it.isNotEmpty() }
            ?: element.get("pageTarget", ""))
            .mapNotNull {
                val href = extractHref(it, filePath)
                val title = extractTitle(it)
                if (href.isNullOrBlank() || title.isNullOrBlank())
                    null
                else Link(title = title, href = href)
            }

    private fun parseNavPointElement(element: ElementNode, filePath: String): Link? {
        val title = extractTitle(element)
        val href = extractHref(element, filePath)
        val children = (element.get("navPoint", Namespaces.NCX).takeIf { it.isNotEmpty() }
            ?: element.get("navPoint", ""))
            .mapNotNull { parseNavPointElement(it, filePath) }
        return if (children.isEmpty() && (href == null || title == null))
            null
        else
            Link(title = title, href = href ?: "#", children = children)
    }

    private fun extractTitle(element: ElementNode): String? {
        val navLabel = element.getFirst("navLabel", Namespaces.NCX)
            ?: element.getFirst("navLabel", "")
        val text = navLabel?.getFirst("text", Namespaces.NCX)
            ?: navLabel?.getFirst("text", "")
        return text?.text?.replace("\\s+".toRegex(), " ")?.trim()?.ifBlank { null }
    }

    private fun extractHref(element: ElementNode, filePath: String): String? {
        val content = element.getFirst("content", Namespaces.NCX)
            ?: element.getFirst("content", "")
        return content?.getAttr("src")
            ?.ifBlank { null }?.let { Href(it, baseHref = filePath).string }
    }
}
