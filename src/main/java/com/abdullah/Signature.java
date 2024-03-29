package com.abdullah;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.CollectionStore;
import org.bouncycastle.util.Store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Collections;

public class Signature implements SignatureInterface {
    private PrivateKey privateKey;
    private X509CertificateHolder certificate;

    public Signature(KeyStore keystore, char[] password){
        try{
            String alias = keystore.aliases().nextElement();
            privateKey = (PrivateKey) keystore.getKey(alias, password);
            certificate = new X509CertificateHolder(keystore.getCertificate(alias).getEncoded());
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public byte[] sign(InputStream content) {
        try{
            // Implement signature generation logic here
            // This is a placeholder and should be replaced with actual signing code
            Security.addProvider(new BouncyCastleProvider());
            SignerInfoGenerator signerInfoGenerator =
                    new JcaSimpleSignerInfoGeneratorBuilder()
                            .setProvider("BC")
                            .build("SHA256withRSA", privateKey, certificate);
            CMSSignedDataGenerator signedDataGenerator = new CMSSignedDataGenerator();

            signedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);

            Store<X509CertificateHolder> certs =
                    new CollectionStore<X509CertificateHolder>(
                            Collections.singletonList(certificate));

            signedDataGenerator.addCertificates(certs);

            byte[] data = new byte[content.available()];
            data = content.readAllBytes();
            CMSTypedData typedData = new CMSProcessableByteArray(data);

            CMSSignedData signedData = signedDataGenerator.generate(typedData);

            return signedData.getEncoded();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }
}
