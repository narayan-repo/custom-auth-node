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


@Node.Metadata(outcomeProvider = SingleOutcomeNode.OutcomeProvider.class,
                configClass = GetConsentIdNode.Config.class)
public class GetConsentIdNode extends SingleOutcomeNode {
	public interface Config {
        @Attribute(order = 100)
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }
        
    }
	
	private static final String BUNDLE = "org/forgerock/openam/auth/nodes/GetConsentIdNode";
	private final GetConsentIdNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
	
	@Inject
    public GetConsentIdNode(@Assisted GetConsentIdNode.Config config) {
        this.config = config;
    }

	@Override
	public Action process(TreeContext context) {
		JsonValue sharedState = context.sharedState;
        return context.getCallback(TextInputCallback.class)
                      .map(TextInputCallback::getText)
                      .map(consentId -> {
                        
                        if (consentId != null && !consentId.isEmpty()) {
                            // Authorization code is present, proceed with success
                            sharedState.put("consentId",consentId);
                            return goToNext().replaceSharedState(sharedState).build();
                        } else {
                            
                            return Action.goTo("failure").build();
                        }
                      }).orElseGet(() -> {
                        
                        TextInputCallback callback = new TextInputCallback("Please enter the Consent Id");
                        return Action.send(callback).build();
                    });
	}
}
