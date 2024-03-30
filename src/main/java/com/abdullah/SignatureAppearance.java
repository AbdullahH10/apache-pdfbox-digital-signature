package com.abdullah;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.security.cert.X509Certificate;
import java.util.List;

public class SignatureAppearance {
    public PDRectangle adjustRectangleForRotation(PDDocument doc, Rectangle2D humanRect)
    {
        float x = (float) humanRect.getX();
        float y = (float) humanRect.getY();
        float width = (float) humanRect.getWidth();
        float height = (float) humanRect.getHeight();
        PDPage page = doc.getPage(0);
        PDRectangle pageRect = page.getCropBox();
        PDRectangle rect = new PDRectangle();
        // signing should be at the same position regardless of page rotation.
        switch (page.getRotation())
        {
            case 90:
                rect.setLowerLeftY(x);
                rect.setUpperRightY(x + width);
                rect.setLowerLeftX(y);
                rect.setUpperRightX(y + height);
                break;
            case 180:
                rect.setUpperRightX(pageRect.getWidth() - x);
                rect.setLowerLeftX(pageRect.getWidth() - x - width);
                rect.setLowerLeftY(y);
                rect.setUpperRightY(y + height);
                break;
            case 270:
                rect.setLowerLeftY(pageRect.getHeight() - x - width);
                rect.setUpperRightY(pageRect.getHeight() - x);
                rect.setLowerLeftX(pageRect.getWidth() - y - height);
                rect.setUpperRightX(pageRect.getWidth() - y);
                break;
            case 0:
            default:
                rect.setLowerLeftX(x);
                rect.setUpperRightX(x + width);
                rect.setLowerLeftY(pageRect.getHeight() - y - height);
                rect.setUpperRightY(pageRect.getHeight() - y);
                break;
        }
        return rect;
    }

    // create a template PDF document with empty signature and return it as a stream.
    public InputStream getVisualSignatureAsStream(
            PDDocument srcDoc,
            PDRectangle rect,
            PDSignature signature,
            File imageFile,
            int pageNum
    ) throws IOException
    {
        try (PDDocument doc = new PDDocument())
        {
            PDPage page = new PDPage(srcDoc.getPage(pageNum).getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            widget.setRectangle(rect);

            // from PDVisualSigBuilder.createHolderForm()
            PDStream stream = new PDStream(doc);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources res = new PDResources();
            form.setResources(res);
            form.setFormType(1);
            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            float height = bbox.getHeight();
            Matrix initialScale = null;
            switch (srcDoc.getPage(pageNum).getRotation())
            {
                case 90:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 180:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(2));
                    break;
                case 270:
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
                    initialScale = Matrix.getScaleInstance(bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                    break;
                case 0:
                default:
                    break;
            }
            form.setBBox(bbox);
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            // from PDVisualSigBuilder.createAppearanceDictionary()
            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);

            try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream))
            {
                // for 90° and 270° scale ratio of width / height
                // not really sure about this
                // why does scale have no effect when done in the form matrix???
                if (initialScale != null)
                {
                    cs.transform(initialScale);
                }

                // show background (just for debugging, to see the rect size + position)
//                cs.setNonStrokingColor(Color.yellow);
//                cs.addRect(-5000, -5000, 10000, 10000);
//                cs.fill();

                if (imageFile != null)
                {
                    // show background image
                    // save and restore graphics if the image is too large and needs to be scaled
                    cs.saveGraphicsState();
                    cs.transform(Matrix.getScaleInstance(0.25f, 0.25f));
                    PDImageXObject img = PDImageXObject.createFromFileByExtension(imageFile, doc);
                    img.setHeight(144);
                    img.setWidth(288);
                    cs.drawImage(img, 0, 0);
                    cs.restoreGraphicsState();
                }

                // show text
                float fontSize = 8;
                float leading = fontSize * 1.5f;
                cs.beginText();
                cs.setFont(font, fontSize);
                cs.setNonStrokingColor(Color.LIGHT_GRAY);
                cs.newLineAtOffset(fontSize, height - leading);
                cs.setLeading(leading);

//                X509Certificate cert = (X509Certificate) getCertificateChain()[0];

                // https://stackoverflow.com/questions/2914521/
//                X500Name x500Name = new X500Name(cert.getSubjectX500Principal().getName());
//                RDN cn = x500Name.getRDNs(BCStyle.CN)[0];
//                String name = IETFUtils.valueToString(cn.getFirst().getValue());

                // See https://stackoverflow.com/questions/12575990
                // for better date formatting
                String name = signature.getName();
                String date = signature.getSignDate().getTime().toString();
                String reason = signature.getReason();

                cs.showText("Digitally signed by "+name);
                cs.newLine();
                cs.showText("On "+date);
                cs.newLine();
                cs.showText("Reason: " + reason);

                cs.endText();
            }

            // no need to set annotations and /P entry

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return new ByteArrayInputStream(baos.toByteArray());
        }
    }
}
