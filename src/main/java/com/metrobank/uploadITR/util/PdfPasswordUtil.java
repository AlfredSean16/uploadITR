package com.metrobank.uploadITR.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;

public class PdfPasswordUtil {
    private static final Logger logger = LoggerFactory.getLogger(PdfPasswordUtil.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
    private static final int PASSWORD_LENGTH = 12;

    public static String generatePassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);

        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }

        return sb.toString();
    }

    public static void encryptPdf(File file, String ownerPassword, String userPassword) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            AccessPermission ap = new AccessPermission();

            // Set permissions
            ap.setCanPrint(true);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);
            ap.setCanAssembleDocument(false);
            ap.setCanFillInForm(false);
            ap.setCanModifyAnnotations(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            spp.setEncryptionKeyLength(256);

            document.protect(spp);
            document.save(file);

            logger.info("PDF encrypted successfully: {}", file.getName());
        } catch (IOException e) {
            logger.error("Failed to encrypt PDF: {}", file.getName(), e);
            throw e;
        }
    }

    public static void reEncryptPdf(File inputFile, String oldUserPassword, File outputFile,
                                    String ownerPassword, String newUserPassword) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputFile, oldUserPassword)) {
            AccessPermission ap = new AccessPermission();

            // Set permissions
            ap.setCanPrint(true);
            ap.setCanModify(false);
            ap.setCanExtractContent(false);
            ap.setCanAssembleDocument(false);
            ap.setCanFillInForm(false);
            ap.setCanModifyAnnotations(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, newUserPassword, ap);
            spp.setEncryptionKeyLength(256);

            document.protect(spp);
            document.save(outputFile);

            logger.info("PDF re-encrypted successfully: {}", outputFile.getName());
        } catch (IOException e) {
            logger.error("Failed to re-encrypt PDF: {}", inputFile.getName(), e);
            throw e;
        }
    }

    public static boolean isPdfPasswordProtected(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            return document.isEncrypted();
        } catch (IOException e) {
            logger.warn("Could not check if PDF is encrypted: {}", file.getName());
            return false;
        }
    }
}