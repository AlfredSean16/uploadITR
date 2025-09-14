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

            ap.setCanPrint(true);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);
            ap.setCanAssembleDocument(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);

            document.protect(spp);
            document.save(file);
        }
    }
    // Re-encrypt an already password-protected PDF
    public static void reEncryptPdf(File inputFile,
                                    String oldUserPassword,
                                    File outputFile,
                                    String ownerPassword,
                                    String newUserPassword) throws IOException {
        try (PDDocument document = PDDocument.load(inputFile, oldUserPassword)) {
            AccessPermission ap = new AccessPermission();

            ap.setCanPrint(true);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);
            ap.setCanAssembleDocument(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, newUserPassword, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);

            document.protect(spp);
            document.save(outputFile);
        }
    }
}
