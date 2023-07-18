package test;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import test.utility.ConsentTestUtility;
import utility.HttpConnection;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class HttpConnectionTest {

    private final String url = "http://localhost:8080/openidm/managed/Consent/";
    private String consentId;
    private String consentIdUrl;
    private JSONObject body;
    private HttpRequest request;
    private HttpResponse<String> response;
    private HttpClient client;

    @Before
    public void setUp() throws Exception {
        client = HttpClient.newBuilder().build();
        body = ConsentTestUtility.createTestConsent("active", "test1234", LocalDateTime.now(), LocalDateTime.now().plusMinutes(15));

        consentId = body.get("_id").toString();
        consentIdUrl = url + consentId;

    }

    @After
    public void tearDown() throws Exception {
        ConsentTestUtility.deleteConsent(consentId);
    }

    @Test
    public void getRequestTest() throws IOException, InterruptedException {

        request = HttpConnection.sendRequest(consentIdUrl, "GET", null, null);
        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //consent found
        assertEquals(200, response.statusCode());

        request = HttpConnection.sendRequest(consentIdUrl + "some_gibberish", "GET", null, null);
        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        //consent not found
        assertEquals(404, response.statusCode());
    }

    @Test
    public void patchRequestTest() throws JSONException, IOException, InterruptedException {

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        request = HttpConnection.sendRequest(consentIdUrl, "PATCH", headers, update());
        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject body = new JSONObject(response.body());

        assertEquals("active", body.get("status").toString());

    }

    private String update() throws JSONException {

        JSONObject jsonObject = new JSONObject(); //updating status

        JSONArray jsonArray = new JSONArray();

        jsonObject.put("operation", "replace");
        jsonObject.put("field", "/status");
        jsonObject.put("value", "active");

        jsonArray.put(jsonObject);

        return jsonArray.toString();
    }

}