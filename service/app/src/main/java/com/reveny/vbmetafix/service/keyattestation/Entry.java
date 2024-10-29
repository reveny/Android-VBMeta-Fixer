package com.reveny.vbmetafix.service.keyattestation;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Entry {
    public static String run() {
        try {
            AttestationResult attestationResult = doAttestation();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Util.ConvertByteArrayToHex(attestationResult.getRootOfTrust().getVerifiedBootHash());
            }
            return "null";
        } catch (Exception e) {
            // Likely TEE broken
            Log.e("Attestation", "Error performing attestation", e);
            return "null";
        }
    }

    private static AttestationResult doAttestation() throws Exception {
        String alias = "reveny";

        Date now = new Date();
        int purposes = KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY;

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, purposes)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setKeyValidityStart(now)
                .setAttestationChallenge(now.toString().getBytes());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        keyPairGenerator.initialize(builder.build());
        keyPairGenerator.generateKeyPair();

        Log.i("Attestation", "Generated key pair");

        List<Certificate> certs = new ArrayList<>();
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        generateKey(alias);
        Certificate[] certificates = keyStore.getCertificateChain(alias);
        if (certificates == null) {
            throw new Exception("Unable to get certificate chain");
        }
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (Certificate cert : certificates) {
            certs.add(cf.generateCertificate(new ByteArrayInputStream(cert.getEncoded())));
        }

        return parseCertificateChain(certs);
    }

    private static void generateKey(String alias) throws Exception {
        Date now = new Date();
        int purposes = KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY;

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias, purposes)
            .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setKeyValidityStart(now)
            .setAttestationChallenge(now.toString().getBytes());

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
        try {
            keyPairGenerator.initialize(builder.build());
            keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            Log.e("Attestation", "TEE is likely broken: ", ex.getCause());
        }
    }

    private static AttestationResult parseCertificateChain(List<Certificate> certs) throws Exception {
        List<X509Certificate> x509Certificates = new ArrayList<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        for (Certificate cert : certs) {
            byte[] encodedCert = cert.getEncoded();
            ByteArrayInputStream bais = new ByteArrayInputStream(encodedCert);
            X509Certificate x509Certificate = (X509Certificate) cf.generateCertificate(bais);
            x509Certificates.add(x509Certificate);
        }

        Log.i("Attestation", "Parsing certificate chain: " + x509Certificates.size() + " certificates");

        return CertificateInfo.parseCertificateChain(x509Certificates);
    }
}
