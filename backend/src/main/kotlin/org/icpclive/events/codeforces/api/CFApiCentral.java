package org.icpclive.events.codeforces.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.icpclive.events.codeforces.api.data.CFSubmission;
import org.icpclive.events.codeforces.api.results.CFStandings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author egor@egork.net
 */
public class CFApiCentral {
    private static final Logger log = LoggerFactory.getLogger(CFApiCentral.class);
    private static final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    private String apiKey;
    private String apiSecret;

    public final int contestId;

    public CFApiCentral(int contestId) {
        this.contestId = contestId;
    }

    public void setApiKeyAndSecret(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public CFStandings getStandings() {
        try {
            JsonNode node = apiRequest("contest.standings", Collections.singletonMap("contestId", String.valueOf(contestId)));
            return mapper.treeToValue(node, CFStandings.class);
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("", e);
            return null;
        }
    }

    public List<CFSubmission> getStatus() {
        try {
            JsonNode node = apiRequest("contest.status", Collections.singletonMap("contestId", String.valueOf(contestId)));
            List<CFSubmission> result = new ArrayList<>();
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                result.add(mapper.treeToValue(elements.next(), CFSubmission.class));
            }
            return result;
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("", e);
            return null;
        }
    }

    private static String hash(String s) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        byte[] bytes = messageDigest.digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte byte_ : bytes) {
            hash.append(Integer.toHexString((byte_ & 0xff) | 0x100), 1, 3);
        }
        return hash.toString();
    }

    private JsonNode apiRequest(String method, Map<String, String> params) throws IOException, NoSuchAlgorithmException {
        SortedMap<String, String> sortedParams = new TreeMap<>(params);

        long time = System.currentTimeMillis() / 1000;
        sortedParams.put("time", String.valueOf(time));
        sortedParams.put("apiKey", apiKey);

        String rand = String.valueOf(random.nextInt(900000) + 100000);
        StringBuilder toHash = new StringBuilder(rand).append("/").append(method).append("?");
        for (Map.Entry<String, String> paramAndValue : sortedParams.entrySet()) {
            toHash.append(paramAndValue.getKey()).append("=").append(paramAndValue.getValue()).append("&");
        }
        toHash.deleteCharAt(toHash.length() - 1).append("#").append(apiSecret);
        sortedParams.put("apiSig", rand + hash(toHash.toString()));

        StringBuilder address = new StringBuilder("https://codeforces.com/api/").append(method).append("?");
        for (Map.Entry<String, String> paramAndValue : sortedParams.entrySet()) {
            address.append(paramAndValue.getKey()).append("=").append(paramAndValue.getValue()).append("&");
        }
        address.deleteCharAt(address.length() - 1);

        URL url = new URL(address.toString());
        JsonNode node = null;
        boolean goon = true;
        while (goon) {
            try {
                node = mapper.readTree(url.openConnection().getInputStream());
                goon = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (node == null || !"OK".equals(node.get("status").asText())) {
            throw new IOException("Request " + address + " unsuccessful");
        }
        node = node.get("result");
        return node;
    }

    public static void main(String[] args) {
        CFApiCentral cfApiCentral = new CFApiCentral(1564);
        cfApiCentral.getStatus();
        cfApiCentral.getStandings();
    }
}
