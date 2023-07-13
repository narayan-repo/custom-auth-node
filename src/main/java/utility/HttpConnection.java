package utility;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

public class HttpConnection {

    private HttpConnection() {
    }

    public static HttpRequest sendRequest(String url, String method, Map<String, String> headersMap,
                                          String requestBody) throws UnsupportedEncodingException {

        HttpRequest request;

        if (headersMap == null || method.equalsIgnoreCase("GET")) {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-OpenIDM-Username", "openidm-admin")
                    .header("X-OpenIDM-Password", "openidm-admin")
                    .GET()
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-OpenIDM-Username", "openidm-admin")
                    .header("X-OpenIDM-Password", "openidm-admin")
                    .headers(getRequestBody(headersMap, requestBody))
                    .method(method.toUpperCase(), HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
        }

        return request;
    }

    private static String getRequestBody(Map<String, String> headersMap, String requestBody) {

        StringBuilder builder = new StringBuilder();
        for (Entry<String, String> header : headersMap.entrySet()) {
            if (header.getKey().equals("Content-Type")
                    && header.getValue().equals("application/x-www-form-urlencoded")) {
                requestBody = URLEncoder.encode(requestBody, StandardCharsets.UTF_8);
            }
            // adding header
            builder.append("\"").append(header.getKey()).append("\"").append(",");
            // adding value
            builder.append("\"").append(header.getValue()).append("\"").append(",");
        }

        //removing comma at the end
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
