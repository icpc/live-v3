package org.icpclive.generator.schema


import com.github.ajalt.clikt.core.*


class MainCommand : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) {
    MainCommand().subcommands(JsonCommand(), TSCommand()).main(args)
}