package org.icpclive.sniper;

public class LocatorPoint {
    final public double x, y, z;

    public LocatorPoint(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocatorPoint move(LocatorPoint d) {
        return new LocatorPoint(x + d.x, y + d.y, z + d.z);
    }

    public LocatorPoint multiply(double d) {
        return new LocatorPoint(x * d, y * d, z * d);
    }

    LocatorPoint rotateZ(double a) {
        return new LocatorPoint(x * Math.cos(a) - y * Math.sin(a),
                x * Math.sin(a) + y * Math.cos(a),
                z);
    }

    public LocatorPoint rotateY(double a) {
        return new LocatorPoint(x * Math.cos(a) - z * Math.sin(a),
                y,
                x * Math.sin(a) + z * Math.cos(a));
    }

    public LocatorPoint rotateX(double a) {
        return new LocatorPoint(x,
                y * Math.cos(a) - z * Math.sin(a),
                y * Math.sin(a) + z * Math.cos(a));
    }
}
