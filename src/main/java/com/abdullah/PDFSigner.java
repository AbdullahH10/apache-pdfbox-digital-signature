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
import java.util.Calendar;

public class PDFSigner {

    private final Signature signature;

    public PDFSigner(Signature signature) throws Exception {
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
        pdSignature.setContactInfo("Contact information");
        pdSignature.setSignDate(Calendar.getInstance());

        // Optional: Add visual signature
        PDVisibleSignDesigner visibleSignDesigner = new PDVisibleSignDesigner(
                doc,
                new FileInputStream("src/main/resources/test/test.png"),
                1
        );
        visibleSignDesigner.coordinates(0f,0f)
                .height(25f)
                .width(25f)
                .adjustForRotation()
                .signatureFieldName("Digital Signature");

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
}

