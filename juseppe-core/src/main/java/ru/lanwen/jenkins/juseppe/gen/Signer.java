package ru.lanwen.jenkins.juseppe.gen;

import net.sf.json.JSONObject;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.jvnet.hudson.crypto.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.lanwen.jenkins.juseppe.beans.Signature;
import ru.lanwen.jenkins.juseppe.beans.UpdateSite;
import ru.lanwen.jenkins.juseppe.gen.json.UpdateSiteSerializer;
import ru.lanwen.jenkins.juseppe.util.SignatureGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.security.Security.addProvider;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.lang3.Validate.isInstanceOf;
import static ru.lanwen.jenkins.juseppe.props.Props.populated;

/**
 * @author Kohsuke Kawaguchi
 */
public class Signer {
    
    public Signer() {
        this.privateKey = new File(populated().getKeyPath());
        this.certificates = new ArrayList<>(Collections.singletonList(new File(populated().getCertPath())));
        this.rootCA = new ArrayList<>(Collections.singletonList(new File(populated().getCertPath())));
    }

    public Signer(String privateKeyPath, List<String> certificatePaths, List<String> rootCAPaths) {
        this.privateKey = new File(privateKeyPath);
        this.certificates = new ArrayList<>(certificatePaths.stream().map(File::new).collect(Collectors.toList()));
        this.rootCA = new ArrayList<>(rootCAPaths.stream().map(File::new).collect(Collectors.toList()));
    }

    private static final Logger LOG = LoggerFactory.getLogger(Signer.class);

    /**
     * Private key to sign the update center. Must be used in conjunction with certificates
     */
    private File privateKey;

    /**
     * X509 certificate for the private key given by the privateKey option.
     * Specify additional certificate options to pass in intermediate certificates, if any
     */
    private List<File> certificates;

    /**
     * Additional root certificates. Should contain your certificate if it self-signed
     */
    private List<File> rootCA;


    /**
     * Checks if the signer is properly configured to generate a signature
     *
     * If the configuration is partial and it's not clear whether the user intended to sign or not to sign.
     */
    public boolean isConfigured() {
        LOG.info("Private key: {}, certificates: {}", privateKey, certificates);
        return privateKey.exists() && certificates.get(0).exists();
    }

    /**
     * Generates a canonicalized JSON format of the given object, and put the signature in it.
     * Because it mutates the signed object itself, validating the signature needs a bit of work,
     * but this enables a signature to be added transparently.
     *
     * @return The same value passed as the argument so that the method can be used like a filter.
     */
    public Signature sign(UpdateSite site) throws GeneralSecurityException, IOException {
        Signature sign = new Signature();

        if (!isConfigured()) {
            LOG.warn("Can't find certificate {} or private key {}, skipping sign", certificates.get(0), privateKey);
            return sign;
        }

        List<X509Certificate> certs = getCertificateChain();
        X509Certificate signer = certs.get(0); // the first one is the signer, and the rest is the chain to a root CA.

        Object o = new PEMParser(new FileReader(privateKey)).readObject();
        PrivateKeyInfo key;
        if(o instanceof PEMKeyPair keyPair) {
            key = keyPair.getPrivateKeyInfo();
        } else if(o instanceof PrivateKeyInfo) {
            key = (PrivateKeyInfo) o;
        } else {
            throw new IllegalArgumentException(String.format("File %s is not rsa private key!", privateKey));
        }

        SignatureGenerator signatureGenerator = new SignatureGenerator(signer, key);

        String marshalledSite = UpdateSiteSerializer.serializer().toJson(site);

        try (OutputStreamWriter output = new OutputStreamWriter(signatureGenerator.getOut(), UTF_8)) {
            JSONObject.fromObject(marshalledSite).writeCanonical(output);
        }

        String digest = signatureGenerator.digest();
        String signature = signatureGenerator.signature();

        // first, backward compatible signature for <1.433 Jenkins that forgets to flush the stream.
        // we generate this in the original names that those Jenkins understands.
        sign.withDigest(digest);
        sign.withCorrectDigest(digest);

        sign.withSignature(signature);
        sign.withCorrectSignature(signature);

        // and certificate chain
        for (X509Certificate cert : certs) {
            sign.getCertificates().add(new String(encodeBase64(cert.getEncoded()), UTF_8));
        }

        return sign;
    }

    /**
     * Loads a certificate chain and makes sure it's valid.
     */
    protected List<X509Certificate> getCertificateChain() throws IOException, GeneralSecurityException {
        CertificateFactory certsFactory = CertificateFactory.getInstance("X509");
        List<X509Certificate> certs = new ArrayList<>();
        for (File certFile : certificates) {
            X509Certificate c = loadCertificate(certsFactory, certFile);
            c.checkValidity(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
            certs.add(c);
        }

        Set<TrustAnchor> rootCAs = CertificateUtil.getDefaultRootCAs();
        for (File f : rootCA) {
            rootCAs.add(new TrustAnchor(loadCertificate(certsFactory, f), null));
        }

        CertificateUtil.validatePath(certs, rootCAs);
        return certs;
    }

    private X509Certificate loadCertificate(CertificateFactory cf, File f) throws CertificateException, IOException {
        try {
            try (FileInputStream in = new FileInputStream(f)) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
                cert.checkValidity();
                return cert;
            }
        } catch (CertificateException | IOException e) {
            throw (IOException) new IOException("Failed to load certificate " + f).initCause(e);
        }
    }

    static {
        addProvider(new BouncyCastleProvider());
    }
}
