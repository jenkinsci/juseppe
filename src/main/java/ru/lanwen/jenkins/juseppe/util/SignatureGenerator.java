package ru.lanwen.jenkins.juseppe.util;

import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.jvnet.hudson.crypto.SignatureOutputStream;

import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import static com.google.common.base.Charsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * @author lanwen (Merkushev Kirill)
 *         Date: 22.06.15
 */
public class SignatureGenerator {
    public static final String SHA_1_WITH_RSA = "SHA1withRSA";
    public static final String SHA_1 = "SHA1";

    private final MessageDigest sha1;
    private final Signature sig;
    private final TeeOutputStream out;
    private final Signature verifier;

    public SignatureGenerator(X509Certificate signer, PrivateKeyInfo key) throws GeneralSecurityException, IOException {
        // this is for computing a digest
        sha1 = MessageDigest.getInstance(SHA_1);
        DigestOutputStream dos = new DigestOutputStream(new NullOutputStream(), sha1);

        // this is for computing a signature
        sig = Signature.getInstance(SHA_1_WITH_RSA);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        sig.initSign(converter.getPrivateKey(key));
        SignatureOutputStream sos = new SignatureOutputStream(sig);

        // this is for verifying that signature validates
        verifier = Signature.getInstance(SHA_1_WITH_RSA);
        verifier.initVerify(signer.getPublicKey());
        SignatureOutputStream vos = new SignatureOutputStream(verifier);

        out = new TeeOutputStream(new TeeOutputStream(dos, sos), vos);
    }

    public TeeOutputStream getOut() {
        return out;
    }

    public MessageDigest getSha1() {
        return sha1;
    }

    public Signature getSig() {
        return sig;
    }

    public Signature getVerifier() {
        return verifier;
    }


    public String digest() {
        byte[] digest = getSha1().digest();
        return new String(encodeBase64(digest), UTF_8);
    }

    public String signature() throws GeneralSecurityException {
        byte[] signature = getSig().sign();
        if (!getVerifier().verify(signature)) {
            throw new GeneralSecurityException(
                    "Signature failed to validate. "
                            + "Either the certificate and the private key weren't matching, or a bug in the program."
            );
        }
        return new String(encodeBase64(signature), UTF_8);
    }

}
