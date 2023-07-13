
package org.forgerock.openam.auth.nodes;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;

import javax.inject.Inject;


import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;

import com.google.inject.assistedinject.Assisted;

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
		@Attribute(order=300)
		default String urlValue()
		{
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
	public Action process(TreeContext context) 
	{
		System.out.println("Client Validation Node");
        
		String jwtToken = context.request.headers.get("authorization").get(0).substring(7);
		String[] parts = jwtToken.split("\\.");
		JSONObject payload;
		JsonValue sharedState = context.sharedState;
		try 
		{
			payload = new JSONObject(new String(Base64.getUrlDecoder().decode(parts[1])));
			String sub=payload.getString("sub");
			sharedState.put("client-id", sub);
			HttpClient client = HttpClient.newBuilder().build();
			System.out.println(sub);
			HttpRequest request = HttpRequest.newBuilder()
			        .uri(URI.create(this.config.urlValue()+sub))
			        .header("X-OpenIDM-Username", "openidm-admin")
			        .header("X-OpenIDM-Password", "openidm-admin")
		            .GET()
			        .build();
			
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			
			if(response.statusCode()==404)
			{
				System.out.println("client invalid");
				//return goTo(false).build();
			}
			
			//actionBuilder.putSessionProperty("client",sub);
		} 
		catch (JSONException | IOException | InterruptedException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
		return goTo(true).replaceSharedState(sharedState).build();

	}
}