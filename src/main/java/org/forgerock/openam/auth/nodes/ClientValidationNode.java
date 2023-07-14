
package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.exception.InvalidClientException;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.HttpConnection;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;

@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = ClientValidationNode.Config.class)
public class ClientValidationNode extends AbstractDecisionNode {

    public interface Config {
        @Attribute(order = 100)
        default String variable() {
            return "variable";
        }

        @Attribute(order = 200)
        default String prompt() {
            return "Prompt";
        }

        @Attribute(order = 300)
        default String urlValue() {
            return "http://localhost:8081/openidm/softwareStatement/getClient/";
        }
    }

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ClientValidationNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    private final ClientValidationNode.Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     *
     * @param config Node configuration.
     */
    @Inject
    public ClientValidationNode(@Assisted ClientValidationNode.Config config) {
        this.config = config;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Action process(TreeContext context) {
        logger.info("Client Validation Node");
        String jwtToken = context.request.headers.get("authorization").get(0).substring(7);
        String[] parts = jwtToken.split("\\.");
        JSONObject payload;
        JsonValue sharedState = context.sharedState;
        try {
            payload = new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1])));
            String sub = payload.getString("sub");
            sharedState.put("client-id", sub);
            HttpClient client = HttpClient.newBuilder().build();
            logger.info("sub: {}", sub);

            HttpRequest request = HttpConnection.sendRequest(config.urlValue() + sub, "GET", null, null);

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            if (response.statusCode() == 404)
                throw new InvalidClientException("Client not found");
        } catch (JSONException | IOException | InterruptedException e) {
            logger.error("Error while parsing: {}", e.getLocalizedMessage());
            return goTo(false).build();
        } catch (InvalidClientException e) {
            logger.error("Client Validation failed: {} ", e.getLocalizedMessage());
            return goTo(false).build();
        }

        logger.info("Client: {} validated successfully", sharedState.get("client-id"));
        return goTo(true).replaceSharedState(sharedState).build();

    }
}