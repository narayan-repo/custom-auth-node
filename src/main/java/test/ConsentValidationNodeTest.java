package test;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.ConsentValidationNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import test.utility.ConsentTestUtility;

import javax.security.auth.callback.Callback;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.junit.Assert.assertEquals;
import static test.utility.ConsentTestUtility.deleteConsent;

public class ConsentValidationNodeTest {

    private final LocalDateTime now = LocalDateTime.now();
    private ConsentValidationNode.Config config;
    private TreeContext context;
    private JsonValue sharedState;
    private JsonValue transientState;
    private String consentId;
    private String _id;

    @Before
    public void setUp() {
        config = Mockito.mock(ConsentValidationNode.Config.class);
        sharedState = json(object(1));
        transientState = json(object(1));

        Mockito.when(config.urlValue()).thenReturn("http://localhost:8080/openidm/endpoint/consent?consentId=");

        consentId = "test1234";
    }

    @After
    public void tearDown() throws Exception {
        sharedState.remove("client-id");
        deleteConsent(_id);
    }

    @Test
    public void consentValidationSuccessfulTest() throws JSONException, IOException, InterruptedException {

        JSONObject body = ConsentTestUtility.createTestConsent("active", consentId, now, now.plusMinutes(15));
        _id = body.getString("_id");

        sharedState.put("client-id", consentId);
        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        ConsentValidationNode node = new ConsentValidationNode(config);
        Action result = node.process(context);

        assertEquals("true", result.outcome);

    }

    @Test
    public void consentExpiredTest() throws JSONException, IOException, InterruptedException {
        JSONObject body = ConsentTestUtility.createTestConsent("active", consentId, now, now.minusMinutes(15));
        _id = body.getString("_id");

        sharedState.put("client-id", consentId);
        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        ConsentValidationNode node = new ConsentValidationNode(config);
        Action result = node.process(context);

        assertEquals("false", result.outcome);
    }

    @Test
    public void consentNotPresentTest() {
        sharedState.put("client-id", consentId);
        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        ConsentValidationNode node = new ConsentValidationNode(config);
        Action result = node.process(context);

        assertEquals("false", result.outcome);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/Consent", sharedState, transientState, new ExternalRequestContext.Builder().build(), callbackList, null);
    }
}