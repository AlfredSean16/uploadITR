package com.metrobank.uploadITR.MailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordEmail(String toEmail, String year, String filename, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("ITR Password for Year " + year);
        message.setText("Dear Employee,\n\n"
                + "Your ITR file for year " + year + " has been uploaded.\n"
                + "Filename: " + filename + "\n"
                + "Password: " + password + "\n\n"
                + "Please keep this password safe.\n\n"
                + "Best regards,\nHR Department");

        mailSender.send(message);
    }
}