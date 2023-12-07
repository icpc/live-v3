package org.icpclive.sniper

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class LocatorPoint(var id: Int, var x: Double, var y: Double, var z: Double, var r: Double = 0.0) {
    constructor(x: Double, y: Double, z: Double, r: Double = 0.0) : this(-1, x, y, z, r)

    fun move(d: LocatorPoint): LocatorPoint {
        return LocatorPoint(id, x + d.x, y + d.y, z + d.z, r + d.r)
    }

    fun multiply(d: Double): LocatorPoint {
        return LocatorPoint(id, x * d, y * d, z * d, r * d)
    }

    fun rotateZ(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x * cos(a) - y * sin(a),
            x * sin(a) + y * cos(a), z, r
        )
    }

    fun rotateY(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x * cos(a) - z * sin(a),
            y, x * sin(a) + z * cos(a), r
        )
    }

    fun rotateX(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x,
            y * cos(a) - z * sin(a),
            y * sin(a) + z * cos(a), a
        )
    }

    fun distTo(o: LocatorPoint): Double {
        return hypot(x - o.x, hypot(y - o.y, z - o.x))
    }

    fun dist(): Double {
        return hypot(x, hypot(y, z))
    }
}