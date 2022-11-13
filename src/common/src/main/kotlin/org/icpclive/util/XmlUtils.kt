package org.icpclive.util

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

fun Element.children() = childNodes.toSequence()
fun Element.children(tag: String) = getElementsByTagName(tag).toSequence()
fun Element.child(tag: String) = getElementsByTagName(tag).toSequence().singleOrNull()
    ?: throw IllegalArgumentException("No child node named $tag")

fun NodeList.toSequence() =
    (0 until length)
        .asSequence()
        .map { item(it) }
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .filterIsInstance<Element>()
