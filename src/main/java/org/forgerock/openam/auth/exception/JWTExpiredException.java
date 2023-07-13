package org.forgerock.openam.auth.exception;

public class JWTExpiredException extends Exception{
    public JWTExpiredException(String message) {
        super(message);
    }
}
