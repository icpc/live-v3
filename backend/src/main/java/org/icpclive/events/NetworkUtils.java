package org.icpclive.events;

import org.slf4j.*;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by aksenov on 15.04.2015.
 */
public class NetworkUtils {
    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    public static void prepareNetwork(final String login, final String password) {
        if (login == null || password == null)
            return;
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }

                }
        };

        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("error", e);
        }


        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        Authenticator.setDefault(
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(login, password.toCharArray());
                    }
                });

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

//        System.setProperty("javax.net.ssl.keyStore", "C:/work/icpc-live/resources/key.jks");
//        System.setProperty("javax.net.ssl.trustStore", "C:/work/icpc-live/resources/key.jks");
    }

    public static InputStream openAuthorizedStream(String url, String login, String password) throws IOException {
        if (!url.contains("http")){
            return new FileInputStream(url);
        }

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        if (login != null) {
            con.setRequestProperty("Authorization",
                    "Basic " + Base64.getEncoder().encodeToString((login + ":" + password).getBytes()));
        }
        con.connect();
        for (Map.Entry<String, List<String>> entry: con.getHeaderFields().entrySet()) {
            log.debug(entry.getKey() + "=" + entry.getValue());
        }
        return con.getInputStream();
    }
}
