package org.forgerock.openam.auth.nodes;

import java.util.Map;

import javax.inject.Inject;
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
                configClass = UpdateAccessTokenNode.Config.class)
public class UpdateAccessTokenNode extends SingleOutcomeNode {
	public interface Config {
        @Attribute(order = 100)
        default String variable() { return "variable"; }

        @Attribute(order = 200)
        default String prompt() { return "Prompt"; }
        
    }
	
	private static final String BUNDLE = "org/forgerock/openam/auth/nodes/UpdateAccessTokenNode";
	private final UpdateAccessTokenNode.Config config;
    private final Logger logger = LoggerFactory.getLogger("amAuth");
	
	@Inject
    public UpdateAccessTokenNode(@Assisted UpdateAccessTokenNode.Config config) {
        this.config = config;
    }

	@Override
	public Action process(TreeContext context) {
		JsonValue sessionSharedState = context.sharedState.get("tokenId");
        JsonValue sessionTransientState = context.transientState.get("tokenId");

        logger.debug(sessionSharedState.asString());
        logger.debug(sessionTransientState.asString());
        return goToNext().build();
	}
}
