package test;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.ConsentStatusUpdateNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import utility.HttpConnection;

import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.junit.Assert.*;
import static test.utility.ConsentTestUtility.createTestConsent;
import static test.utility.ConsentTestUtility.deleteConsent;

public class ConsentStatusUpdateNodeTest {

    private final String url = "http://localhost:8080/openidm/managed/Consent/";
    private final HttpClient client = HttpClient.newBuilder().build();
    private final LocalDateTime now = LocalDateTime.now();
    private TreeContext context;
    private ConsentStatusUpdateNode.Config config;
    private JsonValue sharedState;
    private JsonValue transientState;
    private String consentId;

    @Before
    public void setUp() {

        config = Mockito.mock(ConsentStatusUpdateNode.Config.class);
        sharedState = json(object(1));
        transientState = json(object(1));

        Mockito.when(config.urlValue()).thenReturn(url);

    }

    @After
    public void tearDown() throws Exception {
        deleteConsent(consentId);
    }

    @Test
    public void patchRequestSuccessTest() throws IOException, InterruptedException, JSONException {

        //getting created test consent
        JSONObject body = createTestConsent("inactive", "10101010", now, now.plusMinutes(15));
        consentId = body.get("_id").toString();
        sharedState.put("consentId", consentId);

        //initializing TreeContext
        context = getContext(sharedState, transientState, Collections.emptyList());

        //consent status and statusUpdateDateTime before updating
        assertEquals("inactive", body.get("status").toString());
        assertEquals(now.toString(), body.get("statusUpdateDateTime"));

        ConsentStatusUpdateNode node = new ConsentStatusUpdateNode(config);
        Action result = node.process(context);

        assertNotEquals("false", result.outcome);

        HttpRequest request = HttpConnection.sendRequest(url + consentId, "GET", null, null);
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        body = new JSONObject(response.body());
        assertEquals("active", body.get("status").toString());
        assertTrue(LocalDateTime.parse(body.get("statusUpdateDateTime").toString()).isAfter(now));

    }

    @Test
    public void patchRequestFailedRequest() {

        consentId = "non-existing-consentId";
        sharedState.put("consentId", consentId);

        //initializing TreeContext
        context = getContext(sharedState, transientState, Collections.emptyList());

        ConsentStatusUpdateNode node = new ConsentStatusUpdateNode(config);
        Action result = node.process(context);

        assertEquals("false", result.outcome);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/Consent", sharedState, transientState, new ExternalRequestContext.Builder().build(), callbackList, null);
    }
}