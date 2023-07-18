package utility;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class HttpConnection {

    private HttpConnection() {
    }

    public static HttpRequest sendRequest(String url, String method, Map<String, String> headersMap,
                                          String requestBody) throws UnsupportedEncodingException {

        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(url))
                .header("X-OpenIDM-Username", "openidm-admin")
                .header("X-OpenIDM-Password", "openidm-admin");

        if (method.equalsIgnoreCase("GET")) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            //initializing headers map if null
            if (headersMap == null)
                headersMap = new HashMap<>();

            //adding headers to http-request builder
            for (Entry<String, String> header : headersMap.entrySet()) {
                builder.setHeader(header.getKey(), header.getValue());
            }
            builder.method(method.toUpperCase(),
                    requestBody == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(requestBody));
        }
        return builder.build();
    }

}
