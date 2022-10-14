package org.icpclive.sniper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * @author: pashka
 */
public class PtzTest {

    public static void main(String[] args) throws Exception {
//        try {
//            OnvifDevice nvt = new OnvifDevice("192.168.1.111", "root", "contest");
////            OnvifDevice nvt = new OnvifDevice("192.168.1.111");
//            nvt.getSoap().setLogging(false);
//            List<Profile> profiles = nvt.getDevices().getProfiles();
//            System.out.println(profiles.size());
//            String profileToken = profiles.get(1).getToken();
//
//            System.out.println("!!!" + profileToken);
//
//            PtzDevices ptzDevices = nvt.getPtz();
////
////            while (true) {
////                PTZVector position = ptzDevices.getPosition(profileToken);
////                System.out.println(
////                        "!!! (" + position.getPanTilt().getX() + ", " +
////                                position.getPanTilt().getY() + ") " +
////                                position.getZoom().getX());
////            }
//
////            System.out.println(ptzDevices.isAbsoluteMoveSupported(profileToken));
//            ptzDevices.continuousMove(profileToken, 0, 0, 0);
////            FloatRange panRange = ptzDevices.getPanSpaces(profileToken);
////            FloatRange tiltRange = ptzDevices.getTiltSpaces(profileToken);
////            float zoom = ptzDevices.getZoomSpaces(profileToken).getMin();
//
////            System.out.println("!!!" + panRange + " " + tiltRange + " " + zoom);
//
////            float x = (panRange.getMax() + panRange.getMin()) / 2f;
////            float y = (tiltRange.getMax() + tiltRange.getMin()) / 2f;
////
////            if (ptzDevices.isAbsoluteMoveSupported(profileToken)) {
////                ptzDevices.absoluteMove(profileToken, x, y, zoom);
////            }
//        }
//        catch (ConnectException e) {
//            System.err.println("Could not connect to NVT.");
//        }
//        catch (SOAPException e) {
//            e.printStackTrace();
//        }

        move(0, 0, 0);
//        for (int zoom = 0; zoom < 10000; zoom += 500) {
////            move(0, 0, zoom);
//            double maxmag = 623.0 / 22;
//            double mag = 1 + (maxmag - 1) * zoom / 9999;
//            System.out.println(zoom + " " + mag + " " + 120 / mag);
//            System.out.println(sendGet("http://192.168.1.111/axis-cgi/com/ptz.cgi?query=position,limits&camera=1&html=no&timestamp=" + getUTCTime()));
//        }

//        while (true) {
//            int n = 10;
//            for (int i = 0; i < n; i++) {
//                double a = 2 * Math.PI * i / n;
//                double x = 20 * Math.cos(a);
//                double y = 20 * Math.sin(a);
////                x = 0; y = 0;
////                double z = Math.pow(2, i) * 10;
//                double z = 0;
//                move((float) x, (float) y, (float) z);
//                Thread.sleep(2000);
//            }
//        }

    }

    private static void move(float tilt, float pan, float zoom) throws Exception {
        sendGet("http://192.168.1.112/axis-cgi/com/ptz.cgi?camera=1" +
                "&tilt=" + tilt +
                "&pan=" + pan +
                "&zoom=" + zoom +
                "&timestamp=" + getUTCTime());
    }

    public static String sendGet(String url) throws Exception {
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
