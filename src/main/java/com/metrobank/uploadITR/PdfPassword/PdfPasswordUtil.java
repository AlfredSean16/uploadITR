package com.metrobank.uploadITR.PdfPassword;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

public class PdfPasswordUtil {

    // Generate random password
    public static String generatePassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // Encrypt PDF file with password
    public static void encryptPdf(File file, String ownerPassword, String userPassword) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            AccessPermission ap = new AccessPermission();

            ap.setCanPrint(false);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);

            document.protect(spp);
            document.save(file);
        }
    }
}
