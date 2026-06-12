package fr.gymgod.app.security.gateway;

import fr.gymgod.app.security.domain.port.EmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailGateway implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationEmail(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Vérification de votre compte GymGod");
        message.setText("Votre code de vérification est : " + code + "\n\nCe code expire dans 15 minutes.");

        mailSender.send(message);
    }
}
