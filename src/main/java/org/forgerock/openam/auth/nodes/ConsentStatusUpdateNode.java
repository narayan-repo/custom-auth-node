package org.forgerock.openam.auth.nodes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;


@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = ConsentStatusUpdateNode.Config.class)
public class ConsentStatusUpdateNode extends SingleOutcomeNode {
	public interface Config {
        @Attribute(order = 100)
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }
        
        @Attribute(order=300)
        default String urlValue()
        {
        	return "http://localhost:8081/openidm/endpoint/consent/?consentId=";
        }
        
    }
	
	private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ConsentStatusUpdateNode";
	private final ConsentStatusUpdateNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
	
	@Inject
    public ConsentStatusUpdateNode(@Assisted ConsentStatusUpdateNode.Config config) {
        this.config = config;
    }

    public String update() throws JSONException{
        
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
        
        System.out.println("Consent Status Update Node");
        
        String consentId = context.sharedState.get("consentId").asString();
        System.out.println("Consent Id: "+consentId);
        JsonValue sharedState = context.sharedState;

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(this.config.urlValue() + consentId))
                    .header("X-OpenIDM-Username", "openidm-admin")
                    .header("X-OpenIDM-Password", "openidm-admin")
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(update()))
                    .build();
       

        HttpResponse<String> response = null;

            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject obj = new JSONObject(response.body());
            System.out.println(obj.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Status Not Updated");
        }
        System.out.println("Status Updated");
        return goToNext().build();

    }

    
}
