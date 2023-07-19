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

import com.google.inject.assistedinject.Assisted;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.RSASigningHandler;
import org.forgerock.openam.annotations.sm.Attribute;
import org.forgerock.openam.auth.exception.JWTExpiredException;
import org.forgerock.openam.auth.exception.JWTSignatureException;
import org.forgerock.openam.auth.node.api.AbstractDecisionNode;
import org.forgerock.openam.auth.node.api.Action;
import org.forgerock.openam.auth.node.api.Node;
import org.forgerock.openam.auth.node.api.TreeContext;
import org.forgerock.openam.utils.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

/**
 * A node which collects a username from the user via a name callback.
 *
 * <p>
 * Places the result in the shared state as 'username'.
 * </p>
 */
@Node.Metadata(outcomeProvider = AbstractDecisionNode.OutcomeProvider.class, configClass = ClientAssertionValidatorNode.Config.class)
public class ClientAssertionValidatorNode extends AbstractDecisionNode {

    private static final String BUNDLE = "org/forgerock/openam/auth/nodes/ClientAssertionValidatorNode";
    private final Logger logger = LoggerFactory.getLogger("amAuth");
    private final ClientAssertionValidatorNode.Config config;

    /**
     * Constructs a new SetSessionPropertiesNode instance.
     *
     * @param config Node configuration.
     */
    @Inject
    public ClientAssertionValidatorNode(@Assisted ClientAssertionValidatorNode.Config config) {
        this.config = config;
    }

    private SignedJwt getSignedJwt(String sJWT) throws Exception {
        logger.info("Start of getSignedJwt method");
        SignedJwt signedJWT;
        try {
            JwtReconstruction jwtReconstruction = new JwtReconstruction();
            signedJWT = jwtReconstruction.reconstructJwt(sJWT, SignedJwt.class);
            logger.info("Signed JWT :::: {}", signedJWT);
        } catch (Exception ex) {
            logger.error("Exception occurred while getting signed jwt: {}", ex.getLocalizedMessage());
            throw new Exception("Could not reconstruct JWT");
        }
        logger.info("End of getSignedJwt :::: {}", signedJWT);
        return signedJWT;
    }

    private boolean verifyJWSs(Key publicKey, String signedJWTs) {
        logger.info("Start of verifyJWSs method");
        String signedData = signedJWTs.substring(0, signedJWTs.lastIndexOf("."));
        String signatureB64u = signedJWTs.substring(signedJWTs.lastIndexOf(".") + 1);
        byte[] signature = Base64.getUrlDecoder().decode(signatureB64u);
        RSASigningHandler rsaSigningHandler = new RSASigningHandler(publicKey);
        logger.info("End of verifyJWSs");
        return rsaSigningHandler.verify(JwsAlgorithm.RS256, signedData.getBytes(), signature);
    }

    public PublicKey stringToRSAKey() {
        String publicKeyB64 = BundleUtils.getBundleString(BUNDLE, "publicKeyB64");

        try {
            byte[] byteKey = Base64.getDecoder().decode(publicKeyB64.getBytes());

            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            logger.info("Generated public key: {}", kf.generatePublic(X509publicKey));
            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            logger.error("Error converting string to RSAKey: {}", e.getLocalizedMessage());
            return null;
        }
    }

    public boolean verifyJWTExpiry(SignedJwt signedJWTs) {
        logger.info("Start of verifyJWTExpiry");
        Date jwtExp = (Date) (signedJWTs.getClaimsSet().getClaim("exp"));

        long secsFromEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();
        LocalDateTime ldt = LocalDateTime.ofInstant(jwtExp.toInstant(),
                ZoneId.systemDefault());
        long jwtExpsecs = ldt.atZone(ZoneId.systemDefault()).toInstant().getEpochSecond();

        logger.info("End of verifyJWTExpiry");
        return jwtExpsecs > secsFromEpoch;
    }

    @Override
    public Action process(TreeContext context) {
        String token = context.request.headers.get("authorization").get(0).substring(7);
        String authCode = context.request.headers.get("code").get(0);

        JsonValue sharedState = context.sharedState;
        sharedState.put("authCode", authCode);
        // Create Public key
        PublicKey publicKey;
        SignedJwt signedJwt;

        try {
            publicKey = this.stringToRSAKey();
            logger.info("public key: {}", publicKey);

            //JWT Signature Validation
            boolean validJWTSignature = this.verifyJWSs(publicKey, token);
            if (validJWTSignature) {
                logger.info("JWT Signature is valid");
            } else {
                throw new JWTSignatureException("JWT Signature is invalid");
            }

            //Verify expiry
            signedJwt = getSignedJwt(token);
            boolean validExp = verifyJWTExpiry(signedJwt);
            if (validExp) {
                logger.info("JWT is not expired");
            } else {
                throw new JWTExpiredException("JWT is expired");
            }

        } catch (JWTSignatureException | JWTExpiredException e) {
            logger.error("JWT validation failed: {}", e.getLocalizedMessage());
            return goTo(false).build();
        } catch (Exception e) {
            logger.error("Exception occurred while validating JWT: {}", e.getLocalizedMessage());
            return goTo(false).build();
        }

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
        default Boolean isPassword() {
            return false;
        }

        @Attribute(order = 400)
        default Boolean useTransient() {
            return true;
        }
    }
}