package org.icpclive.sniper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.*;

public class LocatorController {

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    public static final double BASE_RADIUS = 1.1;

    public static void main(String[] args) throws FileNotFoundException {
        Util.init();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                System.out.println("Select sniper (1-" + Util.snipers.size() + ")");
                int sniper = Integer.parseInt(in.readLine().trim());
                Scanner scanner = new Scanner(new File("coordinates-" + sniper + ".txt"));
                int n = scanner.nextInt();
                System.out.println("Select teams (space separated) (1-" + n + ")");
                String[] ss = in.readLine().split("\\s+");
                System.out.println(Arrays.toString(ss));
                int k = ss.length;
                Set<Integer> need = new HashSet<>();
                for (int i = 0; i < k; i++) {
                    need.add(Integer.parseInt(ss[i]));
                }
                List<LocatorPoint> res = new ArrayList<>();
                List<LocatorPoint> allPoints = new ArrayList<>();
                for (int i = 1; i <= n; i++) {
                    LocatorPoint point = new LocatorPoint(
                            scanner.nextInt(),
                            scanner.nextDouble(),
                            scanner.nextDouble(),
                            scanner.nextDouble()
                    );
                    allPoints.add(point);
                    if (need.contains(point.id)) {
                        res.add(point);
                    }
                }
                double D = 1e100;
                for (LocatorPoint p1 : allPoints) {
                    for (LocatorPoint p2 : allPoints) {
                        if (p1 == p2) continue;
                        D = Math.min(D, p1.distTo(p2));
                    }
                }
                res = translatePoints(res, sniper, D);
                String url = "http://172.24.0.173:8080/api/admin/teamLocator/";
                List<String> parts = new ArrayList<>();
                for (LocatorPoint p : res) {
                    parts.add(
                            String.format("{\"x\": %d, \"y\": %d, \"radius\": \"%d\", \"cdsTeamId\": %d}",
                                    (int) p.x, (int) p.y, (int) p.r, p.id));
                }
                System.out.println(parts);
                String data = "{\"circles\": [" + String.join(",", parts) + "]}";
                System.out.println(data);
                Util.sendPost(url + "show_with_settings", "application/json", data);
                System.out.println("Press Enter to hide");
                in.readLine();
                Util.sendPost(url + "hide", "application/json", "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static List<LocatorPoint> translatePoints(List<LocatorPoint> points, int sniper, double d) throws Exception {
        SniperInfo camera = Util.snipers.get(sniper - 1);
        String response = Util.sendGet(
                camera.hostName + "axis-cgi/com/ptz.cgi?query=position,limits&camera=1&html=no&timestamp="
                        + Util.getUTCTime());
        camera.update();
        LocatorConfig config = Util.parseCameraConfiguration(response);
        List<LocatorPoint> res = new ArrayList<>();
        for (LocatorPoint p : points) {
            p.r = d / 2;
            p = p.rotateY(config.pan);
            p = p.rotateX(-config.tilt);
            p = p.multiply(1 / p.z);
            p = p.multiply(WIDTH / config.angle);
            p = p.move(new LocatorPoint(WIDTH / 2, HEIGHT / 2, 0));
            res.add(p);
        }
        return res;
    }

}
