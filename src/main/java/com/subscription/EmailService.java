package com.subscription;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Sends plain-text emails through Gmail SMTP.
 *
 * Credentials are read from environment variables and never written in code:
 *   EMAIL_USERNAME      your Gmail address
 *   EMAIL_APP_PASSWORD  a 16-character Gmail App Password (not your normal password)
 */
public class EmailService {

    private static final String SMTP_HOST = getEnv("SMTP_HOST", "smtp.gmail.com");
    private static final String SMTP_PORT = getEnv("SMTP_PORT", "587");

    private final String username;
    private final Session session;

    /** Throws IllegalStateException with a clear message if the credentials are missing. */
    public EmailService() {
        this.username = System.getenv("EMAIL_USERNAME");
        String appPassword = System.getenv("EMAIL_APP_PASSWORD");

        if (username == null || username.isBlank() || appPassword == null || appPassword.isBlank()) {
            throw new IllegalStateException(
                    "Email is not configured. Set the EMAIL_USERNAME and EMAIL_APP_PASSWORD "
                    + "environment variables before starting the app.");
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");            // we will log in
        props.put("mail.smtp.starttls.enable", "true"); // encrypt the connection
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        // The Authenticator hands the password to the mail library when it connects.
        // The password lives only here, in memory. It is never logged or printed.
        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });
    }

    /** Sends one plain-text email. Throws MessagingException if Gmail rejects it. */
    public void sendEmail(String to, String subject, String body) throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
