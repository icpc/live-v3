package org.icpclive.ksp.common

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.Modifier
import java.io.PrintWriter

class MyCodeGenerator {
    private val builder = StringBuilder()
    private var indentation = 0
    fun build() = builder.toString()
    fun withIndent(block: MyCodeGenerator.() -> Unit) {
        indentation += 2
        block()
        indentation -= 2
    }
    operator fun String.unaryPlus() {
        builder.append(" ".repeat(indentation)).appendLine(this)
    }
    fun appendBuild(block: StringBuilder.() -> Unit) {
        +buildString(block)
    }
    fun addBeforeEol(s: String) {
        if (builder.last() == '\n') {
            builder.deleteAt(builder.lastIndex)
        }
        builder.appendLine(s)
    }
}

fun codeGen(packageName: String, block: MyCodeGenerator.() -> Unit) = MyCodeGenerator().apply {
    +"package $packageName"
    +""
    block()
}.build()

fun MyCodeGenerator.imports(vararg names: String) {
    for (name in names) {
        +"import $name"
    }
    +""
}

fun MyCodeGenerator.withCodeBlock(s: String, block: MyCodeGenerator.() -> Unit) {
    +"$s {"
    withIndent(block)
    +"}"
    +""
}

fun MyCodeGenerator.withParameters(s: String, parameters: MyCodeGenerator.() -> Unit, end: String = "", block: (MyCodeGenerator.()->Unit)? = null) {
    +"$s("
    withIndent(parameters)
    if (block == null) {
        +") $end"
    } else {
        withCodeBlock(") $end", block)
    }
}

fun <T> MyCodeGenerator.withParameters(s: String, parameters: List<T>, render: MyCodeGenerator.(T) -> Unit, end: String = "", block: (MyCodeGenerator.()->Unit)? = null) = withParameters(
    s,
    {
        for (parameter in parameters) {
            render(parameter)
            addBeforeEol(",")
        }
    },
    end,
    block
)

private fun MyCodeGenerator.propertyImpl(
    isVar: Boolean,
    modifiers: List<Modifier>,
    name: String,
    type: String?,
    defaultValue: String?,
    accessors: (MyCodeGenerator.() -> Unit)?
) {
    appendBuild {
        for (modifier in modifiers.sorted()) {
            append(modifier.name.lowercase())
            append(" ")
        }
        if (isVar) {
            append("var ")
        } else {
            append("val ")
        }
        append(name)
        if (type != null) {
            append(": ")
            append(type)
        }
        if (defaultValue != null) {
            append(" = ")
            append(defaultValue)
        }
    }
    if (accessors != null) {
        withIndent {
            accessors()
        }
    }
}
fun MyCodeGenerator.property(modifiers: List<Modifier>, name: String, type: String?, defaultValue: String? = null, accessors: (MyCodeGenerator.() -> Unit)? = null) =
    propertyImpl(false, modifiers, name, type, defaultValue, accessors)
fun MyCodeGenerator.mutableProperty(modifiers: List<Modifier>, name: String, type: String?, defaultValue: String? = null, accessors: (MyCodeGenerator.() -> Unit)? = null) =
    propertyImpl(true, modifiers, name, type, defaultValue, accessors)


fun MyCodeGenerator.ret(value: String) {
    appendBuild {
        append("return ")
        append(value)
    }
}

fun MyCodeGenerator.If(condition: String, block: MyCodeGenerator.() -> Unit) {
    withCodeBlock("if ($condition)") {
        block()
    }
}

fun MyCodeGenerator.serializable(with: String?) {
    appendBuild {
        append("@Serializable")
        if (with != null) {
            append("(with = ")
            append(with)
            append("::class")
            append(")")
        }
    }
}

fun CodeGenerator.generateFile(
    dependencies: Dependencies,
    packageName: String,
    fileName: String,
    extensionName: String = "kt",
    block: MyCodeGenerator.() -> Unit
) = PrintWriter(createNewFile(dependencies, packageName, fileName, extensionName)).use {
    it.println(codeGen(packageName, block))
}
