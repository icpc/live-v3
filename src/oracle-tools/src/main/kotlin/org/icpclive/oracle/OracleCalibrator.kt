@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.icpclive.oracle

import kotlinx.coroutines.runBlocking
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.*
import java.net.URI
import java.nio.file.Path
import java.util.*
import javax.swing.*
import kotlin.io.path.bufferedReader
import kotlin.math.*
import kotlin.system.exitProcess

class OracleCalibrator(private val url: String, private val configPath: Path) : MJpegViewer, MouseListener,
    KeyListener {
    private var image: Image? = null
    var pan = 0.0
    var tilt = 0.0
    var angle = Util.ANGLE
    private val points: MutableList<LocatorPoint?> = ArrayList()
    var frame: JFrame? = null
    var label: JLabel? = null
    var currentTeam = ""
    var currentTeamMonitor = Object()
    private fun run() {
        try {
//            readInput()
            readCoordinates()
            startPlayer()
            synchronized(currentTeamMonitor) {
                while (true) {
                    println("Input team id:")
                    currentTeam = `in`.next()
                    if (currentTeam == "") {
                        points.clear()
                        locations.clear()
                        continue
                    }
                    println("Now locate team $currentTeam")
                    while (currentTeam != "") {
                        currentTeamMonitor.wait()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    private fun startPlayer() {
        image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        object : Thread() {
            override fun run() {
                val runner: MjpegRunner
                try {
                    runner = MjpegRunner(this@OracleCalibrator, URI("$url/mjpg/video.mjpg").toURL())
                    runner.run()
                } catch (e: IOException) {
                    e.printStackTrace()
                    exitProcess(1)
                }
            }
        }.start()
        frame = JFrame()
        label = JLabel()
        draw(image!!.graphics as Graphics2D)
        label!!.icon = ImageIcon(image)
        frame!!.add(label)
        frame!!.pack()
        frame!!.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        label!!.addMouseListener(this)
        frame!!.addKeyListener(this)
        frame!!.isVisible = true
    }

    @Throws(Exception::class)
    private suspend fun updateState() {
        val conf = Util.parseCameraConfiguration(
            Util.oracleRequest(
                url,
                mapOf(
                    "query" to "position,limits",
                    "camera" to 1,
                    "html" to "no",
                    "timestamp" to Util.getUTCTime()
                )
            )
        )
        tilt = conf.tilt
        pan = conf.pan
        angle = conf.angle
    }

    @Synchronized
    override fun setBufferedImage(image: BufferedImage) {
        val g = this.image!!.graphics as Graphics2D
        g.drawImage(image, 0, 0, WIDTH, HEIGHT, null)
        try {
            runBlocking { updateState() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        draw(g)
    }

    @Synchronized
    private fun draw(g: Graphics2D) {
        for (pp in points) {
            var p = pp
            p = p!!.rotateY(pan)
            p = p.rotateX(-tilt)
            val R = (20 / Math.abs(p.z) * Util.ANGLE / angle).toInt() + 5
            p = p.multiply(1 / p.z)
            p = p.multiply(WIDTH / angle)
            p.x *= COMPENSATION_X
            p.y *= COMPENSATION_Y
            p = p.move(LocatorPoint((WIDTH / 2).toDouble(), (HEIGHT / 2).toDouble(), 0.0))
            g.color = Color(255, 0, 0, 150)
            g.fillOval(
                (p.x - R).toInt(), (p.y - R).toInt(),
                (2 * R),
                (2 * R)
            )
        }
    }

    @Synchronized
    override fun repaint() {
        frame!!.repaint()
    }

    override fun setFailedString(s: String) {}
    override fun keyPressed(e: KeyEvent) {
        if (e.keyChar == ' ') click(WIDTH / 2, HEIGHT / 2)
    }

    override fun mouseClicked(e: MouseEvent) {
        val x = e.x
        val y = e.y
        click(x, y)
    }

    class Position {
        var id: String
        var p: LocatorPoint

        constructor(id: String, x: Int, y: Int)  : this(id, x.toDouble(), y.toDouble())

        constructor(id: String, x: Double, y: Double) {
            this.id = id
            p = LocatorPoint(x, y, 0.0)
        }

        constructor(id: String, p: LocatorPoint) {
            this.id = id
            this.p = p
        }
    }

    var input: MutableList<Position> = ArrayList()
    var locations: MutableList<Position> = ArrayList()

    @Throws(IOException::class)
    fun readInput() {
        val reader = configPath.resolve("input.txt").bufferedReader()
        var x = 0
        while (true) {
            val s = reader.readLine() ?: break
            val ss = s.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (y in ss.indices) {
                try {
                    val id = ss[y]
                    input.add(Position(id, x, y))
                } catch (_: NumberFormatException) {
                }
            }
            x++
        }
    }

    /* download file from cds /api/contests/{contest}/teams to opendata.json and then process it using
import json
from contextlib import redirect_stdout
# Open and read the JSON file
with 'opendata.json', 'r') as file:
    data = json.load(file)

with open('input.txt', 'w') as file:
    with redirect_stdout(file):
        for i in range(len(data)):
            print(data[i]['id'], data[i]['location']['x'], data[i]['location']['y'])
     */
    @Throws(IOException::class)
    fun readCoordinates() {
        val regex = "(\\S+) (\\d+\\..\\d) (\\d+\\..\\d*)".toRegex()
        val reader = configPath.resolve("input.txt").bufferedReader()
        reader.use {
            while (true) {
                val s = reader.readLine() ?: break
                val match = regex.matchEntire(s)
                if (match == null || match.groupValues.size != 4) {
                    println("Failed to parse team location info $s, ingorred")
                    continue
                }
                val id = match.groups[1]!!.value
                val x = match.groups[2]!!.value.toDoubleOrNull()
                val y = match.groups[3]!!.value.toDoubleOrNull()
                if (x == null || y == null) {
                    println("Failed to parse team location x or y (not double)")
                    continue
                }
                input.add(Position(id, x, y))
            }
        }
    }

    fun recalculate() {
        if (locations.size < 4) return
        val from: MutableList<Position> = ArrayList()
        val to: MutableList<Position> = ArrayList()
        val mp: MutableMap<String, Position> = HashMap()
        for (position in input) {
            mp[position.id] = position
        }
        for (i in 0..3) {
            val e = locations[locations.size - 1 - i]
            to.add(e)
            val e1 = mp[e.id]
            if (e1 == null) {
                println("no team " + e.id)
                return
            }
            from.add(e1)
        }
        val a = Array(9) { DoubleArray(10) }
        for (i in 0..3) {
            val A = from[i].p
            val B = to[i].p
            a[i * 2][0] = A.x * B.z
            a[i * 2][1] = A.y * B.z
            a[i * 2][2] = B.z
            a[i * 2][6] = -A.x * B.x
            a[i * 2][7] = -A.y * B.x
            a[i * 2][8] = -B.x
            a[i * 2 + 1][3] = A.x * B.z
            a[i * 2 + 1][4] = A.y * B.z
            a[i * 2 + 1][5] = B.z
            a[i * 2 + 1][6] = -A.x * B.y
            a[i * 2 + 1][7] = -A.y * B.y
            a[i * 2 + 1][8] = -B.y
        }
        a[8][9] = 1.0
        a[8][8] = a[8][9]
        for (i in a.indices) {
            var ii = i
            for (t in i until a.size) {
                if (abs(a[t][i]) > abs(a[ii][i])) {
                    ii = t
                }
            }
            val tt = a[i]
            a[i] = a[ii]
            a[ii] = tt
            if (abs(a[i][i]) < 1e-9) throw RuntimeException()
            var k = 1.0 / a[i][i]
            for (j in a[i].indices) {
                a[i][j] *= k
            }
            for (t in a.indices) {
                if (t == i) continue
                k = -a[t][i]
                for (j in a[t].indices) {
                    a[t][j] += k * a[i][j]
                }
            }
        }
        val r = DoubleArray(9)
        for (i in 0..8) r[i] = a[i][9] / a[i][i]
        val ddx = from[1].p.x - from[0].p.x
        val ddy = from[1].p.y - from[0].p.y
        val dx = r[0] * ddx + r[1] * ddy + r[2]
        val dy = r[3] * ddx + r[4] * ddy + r[5]
        val dz = r[6] * ddx + r[7] * ddy + r[8]
        val d = sqrt(dx * dx + dy * dy + dz * dz)
        val d2 = sqrt(ddx * ddx + ddy * ddy)
        for (i in 0..8) {
            r[i] *= d2 / d
        }
        if (sign(r[2]) != sign(points[0]!!.x)) {
            for (i in 0..8) {
                r[i] = -r[i]
            }
        }
        points.clear()
        try {
            val out = PrintWriter(configPath.resolve("output.txt").toFile())
            out.println(input.size)
            for (i in input.indices) {
                val xc = input[i].p.x
                val yc = input[i].p.y
                val xt = r[0] * xc + r[1] * yc + r[2]
                val yt = r[3] * xc + r[4] * yc + r[5]
                val zt = r[6] * xc + r[7] * yc + r[8]
                out.println("${input[i].id} $xt $yt $zt")
                points.add(LocatorPoint(xt, yt, zt))
            }
            out.close()
        } catch (e1: FileNotFoundException) {
            e1.printStackTrace()
        }
    }

    fun click(x: Int, y: Int) {
        synchronized(currentTeamMonitor) {
            if (currentTeam == "") return
            val p = LocatorPoint(x.toDouble(), y.toDouble(), WIDTH / angle)
                .move(LocatorPoint((-WIDTH / 2).toDouble(), (-HEIGHT / 2).toDouble(), 0.0))
                .multiply(angle / WIDTH)
                .rotateX(tilt)
                .rotateY(-pan).let {
                    it.multiply(1 / abs(it.z))
                }
            points.add(p)
            locations.add(Position(currentTeam, p))
            currentTeam = ""
            recalculate()
            val g = image!!.graphics as Graphics2D
            draw(g)
            frame!!.repaint()
            currentTeamMonitor.notifyAll()
        }
    }

    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyReleased(e: KeyEvent) {}

    companion object {
        private const val WIDTH = 1280
        private const val HEIGHT = 720
        private const val COMPENSATION_X = 1.0
        private const val COMPENSATION_Y = 1.0
        private val `in` = Scanner(System.`in`)

        @Throws(FileNotFoundException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val configPath = Path.of("config/oracle-test")
            Util.initForCalibrator(configPath.resolve("oracles.txt").toString(), configPath)
            println("Select oracle (1-" + Util.oracles.size + ")")
            val oracle = `in`.nextInt()
            OracleCalibrator(Util.oracles[oracle - 1].hostName, configPath).run()
        }
    }
}
