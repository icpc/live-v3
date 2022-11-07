package org.icpclive.sniper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class SniperMover {

    static String[] urls;

    public static void main(String[] args) throws Exception {
        {
            Scanner in = new Scanner(new File("snipers.txt"));
            int m = in.nextInt();
            urls = new String[m];
            for (int i = 0; i < m; i++) {
                urls[i] = in.next();
            }
            in.close();
        }
        Locale.setDefault(Locale.US);
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Select sniper (1-" + urls.length + ")");
            int sniper = in.nextInt();
            Scanner scanner = new Scanner(new File("coordinates-" + sniper + ".txt"));
            int n = scanner.nextInt();
            System.out.println("Select team (1-" + n + ")");
            int id = in.nextInt();
            SniperCalibrator.Point point = null;
            for (int i = 1; i <= n; i++) {
                point = new SniperCalibrator.Point(
                        scanner.nextDouble(),
                        scanner.nextDouble(),
                        scanner.nextDouble()
                );
                if (i == id) {
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
            System.out.println(pan + " " + tilt + " " + zoom);
            move(sniper, pan, tilt, (int)zoom);
        }
    }

    private static void move(int sniper, double pan, double tilt, int zoom) throws Exception {
        sendGet(urls[sniper - 1] + "/axis-cgi/com/ptz.cgi?camera=1" +
                "&tilt=" + tilt +
                "&pan=" + pan +
                "&zoom=" + zoom +
                "&timestamp=" + getUTCTime());
    }

    public static String sendGet(String url) throws Exception {
        System.out.println(url);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
//        System.out.println("\nSending 'GET' request to URL : " + url);
//        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        return response.toString();
    }

    public static String getUTCTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(cal.getTime());
        return utcTime;
    }

}
