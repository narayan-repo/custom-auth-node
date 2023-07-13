package org.forgerock.openam.auth.nodes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.inject.Inject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.TextOutputCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;


import com.google.inject.assistedinject.Assisted;


@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = GenerateAccessTokenNode.Config.class)
public class GenerateAccessTokenNode extends SingleOutcomeNode {
	public interface Config {
        @Attribute(order = 100)
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }
        
        @Attribute(order=300)
        default String urlValue() {return "http://openam.demo.com:8080/openam/oauth2/realms/root/realms/demo/access_token"; }
        @Attribute(order=400)
        default String clientId()
        {
        	return "my client id";
        }
        @Attribute(order=500)
        default String clientSecret()
        {
        	return "my client secret";
        }
    }
	
	private static final String BUNDLE = "org/forgerock/openam/auth/nodes/GenerateAccessTokenNode";
	private final GenerateAccessTokenNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    
	
	@Inject
    public GenerateAccessTokenNode(@Assisted GenerateAccessTokenNode.Config config) {
        this.config = config;
    }

	@Override
	public Action process(TreeContext context) {
	    
        System.out.println("Generate Access Token Node");
        
        String authCode = context.sharedState.get("authCode").asString();
        logger.debug("Auth Code is: "+ authCode);
		// Access the shared state
        JsonValue sharedState = context.sharedState;   
		
		
		String url = this.config.urlValue();
        String clientId = this.config.clientId();
        String clientSecret = this.config.clientSecret();
        String authorization = clientId + ":" + clientSecret;
        String encodedAuthorization = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        String grantType = "authorization_code";
        String code = authCode;
        String accessToken = "";
        try {
            URL endpoint = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
            connection.setDoOutput(true);

            String data = "grant_type=" + grantType + "&code=" + code;
            byte[] postData = data.getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(postData);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
           
            System.out.println("Response: " + response.toString());
            
            int startIndex = response.toString().indexOf("\"access_token\":\"");
            if (startIndex != -1) {
                startIndex += 16; // Move to the beginning of the actual access token
                int endIndex = response.toString().indexOf("\"", startIndex);
                if (endIndex != -1) {
                     accessToken = response.toString().substring(startIndex, endIndex);
                }
            }
            else {
            	accessToken = "";
            }
			
            // Store data in the shared state
            sharedState.put("accessToken", accessToken);
            
			System.out.println("Access Token: " + accessToken);
			connection.disconnect();
			
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
        // Callback[] outputCallbacks = new Callback[1];
        //         outputCallbacks[0] = new TextOutputCallback(TextOutputCallback.INFORMATION, "Access Token: "+accessToken);
        // return Action.send(outputCallbacks).replaceSharedState(sharedState).build();
        return goToNext().putSessionProperty("accessToken", accessToken).replaceSharedState(context.sharedState.copy().put(config.variable(), accessToken)).build();
        
	}
}
