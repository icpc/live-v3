package org.icpclive.reacbot

import java.io.Reader
import java.util.concurrent.TimeUnit

private const val timeLimit = 30
private val ffmpegCommand =
    listOf(
        "ffmpeg",
        "-y",
        "-v",
        "error",
        "-i",
        "{inputFile}",
        "-an",
        "-vf",
        "setpts=0.25*PTS, scale=640:-1",
        "{outputFile}"
    )

fun convertVideo(inputFileName: String, outputFileName: String) {
    val process = ProcessBuilder()
        .command(ffmpegCommand.map {
            when (it) {
                "{inputFile}" -> inputFileName
                "{outputFile}" -> outputFileName
                else -> it
            }
        }).start()
    val standardOutput = ProcessOutputBuffer(process.inputStream.reader())
    val errorOutput = ProcessOutputBuffer(process.errorStream.reader())
    Thread(errorOutput).start()
    Thread(standardOutput).start()
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.destroyForcibly()
    }
    if (process.exitValue() != 0) {
        throw FfmpegException(errorOutput.output.trimEnd() + standardOutput.output.trimEnd())
    }
}

class FfmpegException(override val message: String) : RuntimeException()

private class ProcessOutputBuffer(private val inputStream: Reader) : Runnable {
    private val builder = StringBuilder()

    val output: String
        @Synchronized
        get() = builder.toString()

    @Synchronized
    override fun run() {
        try {
            while (true) {
                val buffer = CharArray(1024)
                val readiedChars = inputStream.read(buffer)
                if (readiedChars == -1) {
                    return
                }
                builder.append(buffer, 0, readiedChars.coerceAtMost(1024 - builder.length))
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }
}
