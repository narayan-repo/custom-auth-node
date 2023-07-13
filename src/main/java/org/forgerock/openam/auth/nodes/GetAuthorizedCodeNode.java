package org.forgerock.openam.auth.nodes;

import javax.inject.Inject;
import javax.security.auth.callback.TextInputCallback;

import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.assistedinject.Assisted;

import javassist.compiler.ast.CondExpr;


@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = GetAuthorizedCodeNode.Config.class)
public class GetAuthorizedCodeNode extends SingleOutcomeNode {
	public interface Config {
        @Attribute(order = 100)
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }
        
    }
	
	private static final String BUNDLE = "org/forgerock/openam/auth/nodes/GetAuthorizedCodeNode";
	private final GetAuthorizedCodeNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
	
	@Inject
    public GetAuthorizedCodeNode(@Assisted GetAuthorizedCodeNode.Config config) {
        this.config = config;
    }

	@Override
	public Action process(TreeContext context) {
		
        System.out.println("Authorization Code Node");

        JsonValue sharedState = context.sharedState;
        return context.getCallback(TextInputCallback.class)
                      .map(TextInputCallback::getText)
                      .map(authCode -> {
                        //Check Authorization Code 
                        if (authCode != null && !authCode.isEmpty()) {
                            // Authorization code is present, proceed with success
                            sharedState.put("authCode",authCode);
                            return goToNext().replaceSharedState(sharedState).build();
                        } else {
                            // Authorization code is empty, proceed with failure
                            return Action.goTo("failure").build();
                        }
                      }).orElseGet(() -> {
                        // No TextInputCallback found, send the callback to collect the authorization code
                        TextInputCallback callback = new TextInputCallback("Please enter the authorization code");
                        return Action.send(callback).build();
                    });
	}
}
