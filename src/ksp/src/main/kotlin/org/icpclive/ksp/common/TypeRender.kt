package org.icpclive.ksp.common

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Nullability

fun KSType.render(className: KSType.() -> String) : String = buildString {
    append(className(this@render))
    val typeArgs = arguments
    if (arguments.isNotEmpty()) {
        append("<")
        append(
            typeArgs.map {
                val type = it.type?.resolve()
                "${it.variance.label} ${type?.render(className)}" +
                        if (type?.nullability == Nullability.NULLABLE) "?" else ""
            }.joinToString(", ")
        )
        append(">")
    }
    if (isMarkedNullable) append("?")
}
