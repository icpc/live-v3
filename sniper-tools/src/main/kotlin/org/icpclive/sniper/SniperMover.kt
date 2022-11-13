package org.icpclive.sniper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class SniperMover {

    private static final String DEFAULT_SPEED = "0.52";

    public static void main(String[] args) throws Exception {
        Util.init();
        Locale.setDefault(Locale.US);
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Select sniper (1-" + Util.snipers.size() + ")");
            int sniper = in.nextInt();
            Scanner scanner = new Scanner(new File("coordinates-" + sniper + ".txt"));
            int n = scanner.nextInt();
            System.out.println("Select team (1-" + n + ")");
            int needId = in.nextInt();
            LocatorPoint point = null;
            for (int i = 1; i <= n; i++) {
                int id = scanner.nextInt();
                point = new LocatorPoint(
                        scanner.nextDouble(),
                        scanner.nextDouble(),
                        scanner.nextDouble()
                );
                if (id == needId) {
                    break;
                }
            }
            if (point.y > 0) {
                point.x = -point.x;
                point.y = -point.y;
                point.z = -point.z;
            }
            double tilt = Math.atan2(point.y, Math.hypot(point.x, point.z));
            double pan = Math.atan2(-point.x, -point.z);
            pan *= 180 / Math.PI;
            tilt *= 180 / Math.PI;
            double d = Math.hypot(point.x, Math.hypot(point.y, point.z));
            double mag = 0.5 * d;
            double maxmag = 35;
            double zoom = (mag * 9999 - 1) / (maxmag - 1);
            move(sniper, pan, tilt, (int)zoom);
        }
    }

    private static void move(int sniper, double pan, double tilt, int zoom) throws Exception {
        String hostName = Util.snipers.get(sniper - 1).hostName;
        Util.sendGet(hostName + "/axis-cgi/com/ptz.cgi?camera=1" +
                "&tilt=" + tilt +
                "&pan=" + pan +
                "&zoom=" + zoom +
                "&speed=100" +
                "&timestamp=" + Util.getUTCTime());
        Util.sendGet(hostName + "/axis-cgi/com/ptz.cgi?camera=1" +
                "&speed=" + DEFAULT_SPEED +
                "&timestamp=" + Util.getUTCTime());
    }

}
