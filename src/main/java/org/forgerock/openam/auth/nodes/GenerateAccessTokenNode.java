package org.forgerock.openam.auth.nodes;

import com.google.inject.assistedinject.Assisted;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.exception.AccessTokenGenerationException;
import org.forgerock.openam.auth.node.api.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utility.HttpConnection;

import javax.inject.Inject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
        configClass = GenerateAccessTokenNode.Config.class)
public class GenerateAccessTokenNode extends AbstractDecisionNode{
    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/GenerateAccessTokenNode";
    private final GenerateAccessTokenNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");

    @Inject
    public GenerateAccessTokenNode(@Assisted GenerateAccessTokenNode.Config config) {
        this.config = config;
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
            return "http://openam.example.com:8081/openam/oauth2/realms/root/realms/mobile/access_token";
        }

        @Attribute(order = 400)
        default String clientId() {
            return "my client id";
        }

        @Attribute(order = 500)
        default String clientSecret() {
            return "my client secret";
        }
    }

    @Override
    public Action process(TreeContext context) {

        logger.info("Generate Access Token Node");

        String authCode = context.sharedState.get("authCode").asString();
        logger.info("Auth Code is: " + authCode);
        // Access the shared state
        JsonValue sharedState = context.sharedState;


        String url = this.config.urlValue();
        String clientId = this.config.clientId();
        String clientSecret = this.config.clientSecret();
        String authorization = clientId + ":" + clientSecret;
        String encodedAuthorization = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
        String grantType = "authorization_code";
        String accessToken;
        try {

            String data = "grant_type=" + grantType + "&code=" + authCode;

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Basic " + encodedAuthorization);

            HttpRequest request = HttpConnection.sendRequest(url + "?" + data, "POST", headers, null);
            HttpResponse<String> response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject body = new JSONObject(response.body());
            accessToken = body.get("access_token").toString();

            logger.info("Response: " + response.body());
            System.out.println("Response: " + response.body());

            if (accessToken == null) {
                throw new AccessTokenGenerationException("Access token generation failed");
            }

            // Store data in the shared state
            sharedState.put("accessToken", accessToken);

            logger.info("Access Token: " + accessToken);
            return goTo(true).putSessionProperty("accessToken", accessToken).replaceSharedState(sharedState).build();
        } catch (JSONException | AccessTokenGenerationException e) {
            logger.error(e.getLocalizedMessage());
            return goTo(false).build();
        } catch (Exception e) {
            logger.error("Exception occurred while generating access token: {}", e.getLocalizedMessage());
            return goTo(false).build();
        }

    }
}
