package fr.gymgod.common.exception;

public class InvalidTokenException extends RuntimeException {
    private final String provider;

    public InvalidTokenException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
