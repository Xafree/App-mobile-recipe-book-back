package fr.gymgod.app.security.domain.port;

public interface EmailPort {
    void sendVerificationEmail(String to, String verificationCode);
}
