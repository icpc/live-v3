package org.icpclive.sniper

class LocatorPoint {
    var id: Int
    var x: Double
    var y: Double
    var z: Double
    var r = 0.0

    constructor(id: Int, x: Double, y: Double, z: Double) {
        this.id = id
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(x: Double, y: Double, z: Double) {
        id = -1
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(id: Int, x: Double, y: Double, z: Double, r: Double) {
        this.id = id
        this.x = x
        this.y = y
        this.z = z
        this.r = r
    }

    fun move(d: LocatorPoint): LocatorPoint {
        return LocatorPoint(id, x + d.x, y + d.y, z + d.z, r + d.r)
    }

    fun multiply(d: Double): LocatorPoint {
        return LocatorPoint(id, x * d, y * d, z * d, r * d)
    }

    fun rotateZ(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x * Math.cos(a) - y * Math.sin(a),
            x * Math.sin(a) + y * Math.cos(a), z, r
        )
    }

    fun rotateY(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x * Math.cos(a) - z * Math.sin(a),
            y, x * Math.sin(a) + z * Math.cos(a), r
        )
    }

    fun rotateX(a: Double): LocatorPoint {
        return LocatorPoint(
            id, x,
            y * Math.cos(a) - z * Math.sin(a),
            y * Math.sin(a) + z * Math.cos(a), r
        )
    }

    fun distTo(o: LocatorPoint): Double {
        return Math.hypot(x - o.x, Math.hypot(y - o.y, z - o.z))
    }

    fun dist(): Double {
        return Math.hypot(x, Math.hypot(y, z))
    }
}