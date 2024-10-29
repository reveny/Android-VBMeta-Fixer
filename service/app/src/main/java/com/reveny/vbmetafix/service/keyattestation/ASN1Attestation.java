package com.reveny.vbmetafix.service.keyattestation;

import android.util.Log;

import org.bouncycastle.asn1.ASN1Sequence;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

public class ASN1Attestation extends Attestation {
    static final int ATTESTATION_VERSION_INDEX = 0;
    static final int ATTESTATION_SECURITY_LEVEL_INDEX = 1;
    static final int KEYMASTER_VERSION_INDEX = 2;
    static final int KEYMASTER_SECURITY_LEVEL_INDEX = 3;
    static final int ATTESTATION_CHALLENGE_INDEX = 4;
    static final int UNIQUE_ID_INDEX = 5;
    static final int SW_ENFORCED_INDEX = 6;
    static final int TEE_ENFORCED_INDEX = 7;

    int attestationSecurityLevel;

    public ASN1Attestation(X509Certificate x509Cert) throws CertificateParsingException {
        super(x509Cert);
        ASN1Sequence seq = getAttestationSequence(x509Cert);

        attestationVersion = ASN1Utils.getIntegerFromAsn1(seq.getObjectAt(ATTESTATION_VERSION_INDEX));
        attestationSecurityLevel = ASN1Utils.getIntegerFromAsn1(seq.getObjectAt(ATTESTATION_SECURITY_LEVEL_INDEX));
        keymasterVersion = ASN1Utils.getIntegerFromAsn1(seq.getObjectAt(KEYMASTER_VERSION_INDEX));
        keymasterSecurityLevel = ASN1Utils.getIntegerFromAsn1(seq.getObjectAt(KEYMASTER_SECURITY_LEVEL_INDEX));

        attestationChallenge = ASN1Utils.getByteArrayFromAsn1(seq.getObjectAt(ATTESTATION_CHALLENGE_INDEX));

        uniqueId = ASN1Utils.getByteArrayFromAsn1(seq.getObjectAt(UNIQUE_ID_INDEX));

        try {
            softwareEnforced = new AuthorizationList(seq.getObjectAt(SW_ENFORCED_INDEX));
        } catch (Exception e) {
            Log.e("Attestation", "Error parsing software enforced attestation extension", e);
        }

        try {
            teeEnforced = new AuthorizationList(seq.getObjectAt(TEE_ENFORCED_INDEX));
        } catch (Exception e) {
            Log.e("Attestation", "Error parsing tee enforced attestation extension", e);
        }
    }

    ASN1Sequence getAttestationSequence(X509Certificate x509Cert) throws CertificateParsingException {
        byte[] attestationExtensionBytes = x509Cert.getExtensionValue(Attestation.ASN1_OID);
        if (attestationExtensionBytes == null || attestationExtensionBytes.length == 0) {
            throw new CertificateParsingException("Did not find extension with OID " + ASN1_OID);
        }
        return ASN1Utils.getAsn1SequenceFromBytes(attestationExtensionBytes);
    }

    public int getAttestationSecurityLevel()
    {
        return attestationSecurityLevel;
    }

    public RootOfTrust getRootOfTrust() {
        RootOfTrust tee = teeEnforced.getRootOfTrust();
        if (tee != null) return tee;
        return softwareEnforced.getRootOfTrust();
    }
}
