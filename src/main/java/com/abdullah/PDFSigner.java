package com.abdullah;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSigProperties;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.PDVisibleSignDesigner;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
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

        PDDocument doc = Loader.loadPDF(
                new RandomAccessReadBufferedFile(inputFile)
        );
        PDPage refPage = doc.getPage(0);

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
        File sigImage = new File("src/main/resources/test/test.png");
        SignatureAppearance signatureAppearance = new SignatureAppearance();
        Rectangle sigRect = new Rectangle(0,(int)(refPage.getMediaBox().getHeight()-72),144,72);
        PDRectangle sigPDRect = signatureAppearance.adjustRectangleForRotation(doc,sigRect);
        InputStream visualSignStream = signatureAppearance.getVisualSignatureAsStream(
                doc,
                sigPDRect,
                pdSignature,
                sigImage,
                1
        );

        signatureOptions.setVisualSignature(visualSignStream);
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

