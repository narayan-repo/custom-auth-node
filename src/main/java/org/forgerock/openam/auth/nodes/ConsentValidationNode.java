package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.HttpConnection;

import javax.inject.Inject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;


@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = ConsentValidationNode.Config.class)
public class ConsentValidationNode extends AbstractDecisionNode {
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ConsentValidationNode";
    private final ConsentValidationNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    @Inject
    public ConsentValidationNode(@Assisted ConsentValidationNode.Config config) {
        this.config = config;
    }

    @Override
    public Action process(TreeContext context) {

        logger.info("Consent Validation Node");

        String clientId = context.sharedState.get("client-id").asString();
        logger.info("Client Id: {}", clientId);

        JsonValue sharedState = context.sharedState;

        HttpRequest request;

        HttpResponse<String> response;

        try {
            request = HttpConnection.sendRequest(config.urlValue() + clientId, "GET", null, null);
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());
            JSONArray arr = obj.getJSONArray("ClientDetails");

            logger.info("ClientDetails: {}", arr.toString());
            String consentId = arr.getJSONObject(0).getString("_id");

            if (consentId == null || consentId.isEmpty()) {
                logger.warn("No Consent Id found for the client");
                return goTo(false).build();
            }
            //Storing consentId in Consent State
            sharedState.put("consentId", consentId);

            //Expiry Validation of Consent
            String consentExpiryDateTime = arr.getJSONObject(0).getString("consentExpiryDateTime");
            logger.info("consentExpiryDateTime: {}", consentExpiryDateTime);

            if (LocalDateTime.now().isAfter(LocalDateTime.parse(consentExpiryDateTime))) {
                logger.warn("Consent is expired");
                return goTo(false).build();
            }

        } catch (Exception e) {
            logger.error("Error validating consent: {}", e.getLocalizedMessage());
            return goTo(false).build();
        }
        logger.info("Success");
        return goTo(true).replaceSharedState(sharedState).build();
    }

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
            return "http://localhost:8081/openidm/endpoint/consent?clientName=";
        }

    }

}
