package test;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.GenerateAccessTokenNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.junit.Assert.assertNotEquals;

public class GenerateAccessTokenNodeTest {

    private GenerateAccessTokenNode.Config config;
    private JsonValue sharedState;
    private JsonValue transientState;

    @Before
    public void setUp() {
        config = Mockito.mock(GenerateAccessTokenNode.Config.class);
        sharedState = json(object(1));
        transientState = json(object(1));
    }

    @Test
    public void process() {
        String authCode = "wWKbTAASK_oFft23bu2lVd-HqNM";
        sharedState.put("authCode", authCode);

        TreeContext context = getContext(sharedState, transientState, Collections.emptyList());

        Mockito.when(config.urlValue()).thenReturn("http://openam.example.com:8081/openam/oauth2/realms/root/realms/mobile/access_token");
        Mockito.when(config.clientId()).thenReturn("test");
        Mockito.when(config.clientSecret()).thenReturn("test");

        GenerateAccessTokenNode node = new GenerateAccessTokenNode(config);

        Action result = node.process(context);

        assertNotEquals("failure", result.outcome);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, List<Callback> callbackList) {
        return new TreeContext("managed/softwareStatement", sharedState, transientState, new ExternalRequestContext.Builder().build(), callbackList, null);
    }
}