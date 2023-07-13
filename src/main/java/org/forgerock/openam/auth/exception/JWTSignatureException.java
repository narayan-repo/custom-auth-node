package org.forgerock.openam.auth.exception;

public class JWTSignatureException extends Exception{
    public JWTSignatureException(String message) {
        super(message);
    }
}
