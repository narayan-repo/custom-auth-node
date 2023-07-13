package test;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.ExternalRequestContext;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.auth.nodes.ClientValidationNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.security.auth.callback.Callback;
import java.util.Collections;
import java.util.List;

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientValidationNode.class)
class ClientValidationNodeTest {

    private final String validJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjNWM3MmZhMy05Y2Y0LTRmZjctYWM4YS0xZTRiYzFiNjQ4NWMiLCJuYW1lIjoiSm9obiBEb2UiLCJpYXQiOjE1MTYyMzkwMjJ9.rs-h93j4s5QMfaqo_4EAPGXGesmMXQFPfa7tJ6oy7Ks";
    private final String inValidJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjNWM3MmZhMy05Y2Y0LWZkczMyNGZmNy1hYzhhLTFlNGJjMWI2NDg1YyIsIm5hbWUiOiJKb2huIERvZSIsImlhdCI6MTUxNjIzOTAyMn0.9T_FLlTLYY8VWR7qUngjK-gYbb-4K3RLg_dF2NfGC_M";
    private ClientValidationNode.Config config;
    private JsonValue sharedState;
    private JsonValue transientState;
    ListMultimap<String, String> headers;

    @BeforeEach
    public void before() {
        MockitoAnnotations.openMocks(this);
        config = Mockito.mock(ClientValidationNode.Config.class);
        sharedState = json(object(1));
        transientState = json(object(1));
        headers = LinkedListMultimap.create();
    }

    @Test
    void testValidClient() {
        Action result = verifyJwtToken(validJwtToken);
        Assertions.assertEquals("true", result.outcome);
    }

    @Test
    void testInvalidClient() {
        Action result = verifyJwtToken(inValidJwtToken);
        Assertions.assertEquals("false", result.outcome);
    }

    private Action verifyJwtToken(String inValidJwtToken) {
        headers.put("authorization", "Bearer " + inValidJwtToken);

        ExternalRequestContext request = new ExternalRequestContext.Builder().headers(headers).build();
        TreeContext context = getContext(sharedState, transientState, request, Collections.emptyList());

        Mockito.when(config.urlValue()).thenReturn("http://localhost:8080/openidm/managed/softwareStatement/");

        ClientValidationNode node = new ClientValidationNode(config);

        return node.process(context);
    }

    private TreeContext getContext(JsonValue sharedState, JsonValue transientState, ExternalRequestContext request, List<Callback> callbackList) {
        return new TreeContext("managed/softwareStatement", sharedState, transientState, request, callbackList, null);
    }
}
