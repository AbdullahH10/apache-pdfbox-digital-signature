package com.abdullah;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class Main {
    public static void main(String[] args) {
        try {
            File inputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/test/sample.pdf");
            File outputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/signed/test.pdf");

            // Load the keystore
            FileInputStream pkcs12Stream = new FileInputStream("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/test/test.pfx");
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            char[] password = "12345".toCharArray();
            keystore.load(pkcs12Stream, password);
            pkcs12Stream.close();

            //create a Signature class instance which implements SignatureInterface
            Signature signature = new Signature(keystore,password);

            // Set signature options
            SignatureOptions signatureOptions = new SignatureOptions();
            signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE);

            SignPDF signPdf = new SignPDF(signature);
            signPdf.sign(inputFile, outputFile, signatureOptions);

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}