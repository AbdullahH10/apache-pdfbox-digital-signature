package com.abdullah;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Calendar;

public class SignPDF {

    private final Signature signature;

    public SignPDF(Signature signature) throws Exception {
        this.signature = signature;
    }

    public void sign(File inputFile, File outputFile, SignatureOptions signatureOptions) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Document for signing does not exist");
        }

        FileInputStream fis = new FileInputStream(inputFile);
        PDDocument doc = PDDocument.load(fis);
        fis.close();

        // Create signature dictionary
        PDSignature pdSignature = new PDSignature();
        pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        pdSignature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
        pdSignature.setName("Your Name");
        pdSignature.setLocation("Your Location");
        pdSignature.setReason("Your Reason for Signing");
        pdSignature.setSignDate(Calendar.getInstance());

        // Optional: Add visual signature
        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(
                doc,
                new FileInputStream("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/test/test.png"),
                1
        );
        visibleSignDesigner.xAxis(30f)
                .yAxis(0f)
                .height(25f)
                .width(25f)
                .adjustForRotation()
                .signatureFieldName("signature");

        PDVisibleSigProperties visibleSigProperties = new PDVisibleSigProperties();
        visibleSigProperties.setPdVisibleSignature(visibleSignDesigner);
        visibleSigProperties.buildSignature();

        signatureOptions.setVisualSignature(visibleSigProperties);
        signatureOptions.setPage(0);

        // Add signature field to the document
        doc.addSignature(pdSignature, signature, signatureOptions);

        // Write the document to output file
        FileOutputStream fos = new FileOutputStream(outputFile);
        doc.saveIncremental(fos);
        doc.close();
        fos.close();
    }

    public static void main(String[] args) {
        try {
//            File inputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/test/sample.pdf");
            File inputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/test/test_drawing.pdf");
//            File inputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/signed/test.pdf");
            File outputFile = new File("/mnt/Fast Storage/Projects/apache-pdfbox-digital-signature/src/main/resources/signed/test2.pdf");

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

