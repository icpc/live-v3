package org.icpclive.sniper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class Util {

    public static List<SniperInfo> snipers = new ArrayList<>();

    public static final double ANGLE = 1.28;

    public static String sendGet(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static String getUTCTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d'T'HH:mm:ss'Z'");
        sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"));

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        String utcTime = sdf.format(cal.getTime());
        return utcTime;
    }

    public static LocatorConfig parseCameraConfiguration(String s) {
        s = s.trim();
        int l = 0;
        int r = 0;
        double newPan = Double.NaN;
        double newTilt = Double.NaN;
        double newAngle = Double.NaN;
        while (r < s.length()) {
            l = r;
            r = l + 1;
            while (r < s.length() && Character.isAlphabetic(s.charAt(r))) {
                r++;
            }
            String key = s.substring(l, r);
            l = r + 1;
            r = l + 1;
            while (r < s.length() && !Character.isAlphabetic(s.charAt(r))) {
                r++;
            }
            try {
                double value = Double.parseDouble(s.substring(l, r));
                switch (key) {
                    case "pan":
                        newPan = value * Math.PI / 180;
                        break;
                    case "tilt":
                        newTilt = value * Math.PI / 180;
                        break;
                    case "zoom":
                        double maxmag = 35;
                        double mag = 1 + (maxmag - 1) * value / 9999;
                        newAngle = ANGLE / mag;
                        break;
                }
            } catch (Exception e) {
            }
        }
        if (Double.isNaN(newPan) || Double.isNaN(newTilt) || Double.isNaN(newAngle)) {
            throw new AssertionError();
        }
        return new LocatorConfig(newPan, newTilt, newAngle);
    }

    public static void init() {
        try {
            Scanner in = new Scanner(new File("snipers.txt"));
            int m = in.nextInt();
            String[] urls = new String[m];
            for (int i = 0; i < m; i++) {
                urls[i] = in.next();
            }
            in.close();
            for (int i = 0; i < urls.length; i++) {
                snipers.add(new SniperInfo(urls[i],
                        new File("coordinates-" + (i + 1) + ".txt"), i + 1));
            }
        } catch (FileNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    public static void sendPost(String urlString, String contentType, String data) throws IOException {
        URL url = new URL(urlString);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection)con;
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", contentType);
        http.setDoOutput(true);

        byte[] out = data.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.connect();
        OutputStream os = http.getOutputStream();
        os.write(out);
        os.close();
    }
}
