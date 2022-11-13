package org.icpclive.sniper;

public class LocatorPoint {

    public int id;
    public double x, y, z, r;

    public LocatorPoint(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocatorPoint(double x, double y, double z) {
        this.id = -1;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocatorPoint(int id, double x, double y, double z, double r) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
    }

    public LocatorPoint move(LocatorPoint d) {
        return new LocatorPoint(id, x + d.x, y + d.y, z + d.z, r + d.r);
    }

    public LocatorPoint multiply(double d) {
        return new LocatorPoint(id, x * d, y * d, z * d, r * d);
    }

    LocatorPoint rotateZ(double a) {
        return new LocatorPoint(id, x * Math.cos(a) - y * Math.sin(a),
                x * Math.sin(a) + y * Math.cos(a), z, r);
    }

    public LocatorPoint rotateY(double a) {
        return new LocatorPoint(id, x * Math.cos(a) - z * Math.sin(a),
                y, x * Math.sin(a) + z * Math.cos(a), r);
    }

    public LocatorPoint rotateX(double a) {
        return new LocatorPoint(
                id, x,
                y * Math.cos(a) - z * Math.sin(a),
                y * Math.sin(a) + z * Math.cos(a), r);
    }

    public double distTo(LocatorPoint o) {
        return Math.hypot(x - o.x, Math.hypot(y - o.y, z - o.z));
    }

    public double dist() {
        return Math.hypot(x, Math.hypot(y, z));
    }
}
