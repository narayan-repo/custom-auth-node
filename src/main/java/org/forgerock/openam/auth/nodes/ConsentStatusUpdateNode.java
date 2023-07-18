package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.exception.ConsentNotFoundException;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.HttpConnection;

import javax.inject.Inject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class, configClass = ConsentStatusUpdateNode.Config.class)
public class ConsentStatusUpdateNode extends SingleOutcomeNode {
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ConsentStatusUpdateNode";
    private final ConsentStatusUpdateNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    @Inject
    public ConsentStatusUpdateNode(@Assisted ConsentStatusUpdateNode.Config config) {
        this.config = config;
    }

    public String update() throws JSONException {

        LocalDateTime now = LocalDateTime.now();

        JSONObject jsonObject1 = new JSONObject(); //updating status
        JSONObject jsonObject2 = new JSONObject(); //updating status update time

        JSONArray jsonArray = new JSONArray();

        jsonObject1.put("operation", "replace");
        jsonObject1.put("field", "/status");
        jsonObject1.put("value", "active");

        jsonObject2.put("operation", "replace");
        jsonObject2.put("field", "/statusUpdateDateTime");
        jsonObject2.put("value", now.toString());

        jsonArray.put(jsonObject1);
        jsonArray.put(jsonObject2);

        return jsonArray.toString();
    }

    @Override
    public Action process(TreeContext context) {

        logger.info("Consent Status Update Node");

        String consentId = context.sharedState.get("consentId").asString();
        logger.info("Consent Id: {}", consentId);

        HttpRequest request;
        HttpResponse<String> response;
        try {

            Map<String, String> headersMap = new HashMap<>();
            headersMap.put("Content-Type", "application/json");

            request = HttpConnection.sendRequest(this.config.urlValue() + consentId, "PATCH", headersMap, update());
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                throw new ConsentNotFoundException("Consent with consentId: " + consentId + "does not exist");
            }

            JSONObject obj = new JSONObject(response.body());
            logger.info(obj.toString());
            System.out.println(response.body());

        } catch (ConsentNotFoundException e) {
            logger.warn("Consent update failed: {}", e.getLocalizedMessage());
            return Action.goTo("false").build();
        } catch (Exception e) {
            logger.error("Error updating consent: {}", e.getLocalizedMessage());
            return Action.goTo("false").build();
        }
        logger.info("Status Updated");
        return goToNext().build();

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
            return "http://localhost:8081/openidm/endpoint/consent/?consentId=";
        }

    }

}
