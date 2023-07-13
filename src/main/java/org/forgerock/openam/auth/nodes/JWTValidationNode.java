/*
 * jon.knight@forgerock.com
 *
 * Sets user profile attributes 
 *
 */

/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2017 ForgeRock AS.
 */

 package org.forgerock.openam.auth.nodes;

 import java.io.IOException;
 import java.net.URI;
 import java.net.http.HttpClient;
 import java.net.http.HttpRequest;
 import java.net.http.HttpResponse;
 import java.net.http.HttpResponse.BodyHandlers;
 import java.text.ParseException;
 import java.util.Date;
 import java.util.GregorianCalendar;
 
 import javax.inject.Inject;
 
 import org.forgerock.http.protocol.Request;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.annotations.sm.Attribute;
 import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
 import org.forgerock.openam.auth.node.api.Action;
 import org.forgerock.openam.auth.node.api.Action.ActionBuilder;
 import org.forgerock.openam.auth.node.api.Node;
 import org.forgerock.openam.auth.node.api.SingleOutcomeNode;
 import org.forgerock.openam.auth.node.api.TreeContext;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.google.inject.assistedinject.Assisted;
 
 /**
  * A node which collects a username from the user via a name callback.
  *
  * <p>Places the result in the shared state as 'username'.</p>
  */
 @Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class,
             configClass = JWTValidationNode.Config.class)
 public class JWTValidationNode extends AbstractDecisionNode {

     public interface Config {
         @Attribute(order = 100)
         default String variable() { return "variable"; }
 
         @Attribute(order = 200)
         default String prompt() { return "Prompt"; }
 
     }
 
     private static final String BUNDLE = "org/forgerock/openam/auth/nodes/JWTValidationNode";
     private final Logger logger = LoggerFactory.getLogger("amAuth");
 
     private final JWTValidationNode.Config config;
 
     /**
      * Constructs a new SetSessionPropertiesNode instance.
      * @param config Node configuration.
      */
     @Inject
     public JWTValidationNode(@Assisted JWTValidationNode.Config config) {
         this.config = config;
     }
 
 
     @Override
     public Action process(TreeContext context) 
     {
 
         System.out.println("JWT Validation Node");
         String jwtToken = context.request.headers.get("authorization").get(0).substring(7);
  
 
         System.out.println(jwtToken);
        JsonValue sharedState = context.sharedState;
        sharedState.put("jwtToken", jwtToken);

         HttpClient client = HttpClient.newBuilder().build();
         
         
         try 
         {
             
             System.out.println("inside");
             HttpRequest validateJwt = HttpRequest.newBuilder()
                     .uri(URI.create("http://localhost:8084/validateJwt/"+jwtToken))
                     .GET()
                     .build();
             
             String result=client.send(validateJwt, BodyHandlers.ofString()).body();
             System.out.println(result);
             if(!result.equals("true"))
             {
                 System.out.println("failure");
                 return goTo(false).build();
             }
             
         } 
         catch (Exception e) 
         {
             // TODO Auto-generated catch block
             e.printStackTrace();
         }
         
             
         
         System.out.println("success");
         return goTo(true).replaceSharedState(sharedState).build();
             
         
     }
 }