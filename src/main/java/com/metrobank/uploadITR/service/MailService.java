package com.metrobank.uploadITR.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordEmail(String toEmail, String userName, Integer year, String filename, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("MetroBank eITR - Password for ITR Year " + year);
            message.setText(buildEmailContent(userName, year, filename, password));

            mailSender.send(message);
            logger.info("Password email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password email: " + e.getMessage());
        }
    }

    public void sendItrUploadNotification(String toEmail, String userName, Integer year, String filename) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("MetroBank eITR - ITR Upload Confirmation for Year " + year);
            message.setText(buildUploadNotificationContent(userName, year, filename));

            mailSender.send(message);
            logger.info("Upload notification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send upload notification email to: {}", toEmail, e);
            // Don't throw exception for notification emails
        }
    }

    public void sendItrUpdateNotification(String toEmail, String userName, Integer year, String filename, boolean passwordChanged) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("MetroBank eITR - ITR Update Confirmation for Year " + year);
            message.setText(buildUpdateNotificationContent(userName, year, filename, passwordChanged));

            mailSender.send(message);
            logger.info("Update notification email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send update notification email to: {}", toEmail, e);
            // Don't throw exception for notification emails
        }
    }

    private String buildEmailContent(String userName, Integer year, String filename, String password) {
        return String.format(
                "Dear %s,\n\n" +
                        "Your ITR document for year %d has been successfully uploaded to the MetroBank eITR System.\n\n" +
                        "Document Details:\n" +
                        "- Year: %d\n" +
                        "- Filename: %s\n" +
                        "- PDF Password: %s\n\n" +
                        "IMPORTANT SECURITY NOTES:\n" +
                        "• Please store this password securely\n" +
                        "• Do not share this password with unauthorized personnel\n" +
                        "• You will need this password to access your ITR document\n" +
                        "• This password is unique to your document\n\n" +
                        "If you have any questions or concerns, please contact the HR Department immediately.\n\n" +
                        "Best regards,\n" +
                        "MetroBank HR Department\n" +
                        "eITR System",
                userName != null ? userName : "Employee", year, year, filename, password
        );
    }

    private String buildUploadNotificationContent(String userName, Integer year, String filename) {
        return String.format(
                "Dear %s,\n\n" +
                        "This is to confirm that your ITR document for year %d has been successfully uploaded.\n\n" +
                        "Document Details:\n" +
                        "- Year: %d\n" +
                        "- Filename: %s\n" +
                        "- Upload Date: %s\n\n" +
                        "Your document has been securely stored and is now available in the system.\n" +
                        "A separate email with the document password will be sent shortly.\n\n" +
                        "Best regards,\n" +
                        "MetroBank HR Department\n" +
                        "eITR System",
                userName != null ? userName : "Employee", year, year, filename,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );
    }

    private String buildUpdateNotificationContent(String userName, Integer year, String filename, boolean passwordChanged) {
        String passwordInfo = passwordChanged
                ? "A new password has been generated and will be sent in a separate email."
                : "The existing password remains unchanged.";

        return String.format(
                "Dear %s,\n\n" +
                        "Your ITR document for year %d has been successfully updated.\n\n" +
                        "Updated Document Details:\n" +
                        "- Year: %d\n" +
                        "- Filename: %s\n" +
                        "- Update Date: %s\n\n" +
                        "Password Information: %s\n\n" +
                        "Best regards,\n" +
                        "MetroBank HR Department\n" +
                        "eITR System",
                userName != null ? userName : "Employee", year, year, filename,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                passwordInfo
        );
    }
}