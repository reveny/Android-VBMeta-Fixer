package com.reveny.vbmetafix.service.keyattestation;

import static com.reveny.vbmetafix.service.keyattestation.Attestation.KM_SECURITY_LEVEL_SOFTWARE;

import java.util.ArrayList;
import java.util.List;

public class AttestationResult {
    private final List<CertificateInfo> certs;
    private RootOfTrust rootOfTrust;
    private int status = CertificateInfo.KEY_FAILED;
    private boolean sw = true;
    public Attestation showAttestation;

    private AttestationResult(ArrayList<CertificateInfo> certs)
    {
        this.certs = certs;
    }

    public List<CertificateInfo> getCerts()
    {
        return certs;
    }

    public RootOfTrust getRootOfTrust()
    {
        return rootOfTrust;
    }

    public int getStatus()
    {
        return status;
    }

    public boolean isSoftwareLevel()
    {
        return sw;
    }

    public static AttestationResult form(ArrayList<CertificateInfo> certs) throws Exception {
        AttestationResult result = new AttestationResult(certs);
        result.status = certs.get(0).getIssuer();

        for (CertificateInfo cert : certs) {
            if (cert.getStatus() < CertificateInfo.CERT_EXPIRED) {
                result.status = CertificateInfo.KEY_FAILED;
                break;
            }
        }

        CertificateInfo info = certs.get(certs.size() - 1);
        Attestation attestation = info.getAttestation();
        if (attestation != null) {
            result.showAttestation = attestation;
            result.rootOfTrust = attestation.getRootOfTrust();
            result.sw = attestation.getAttestationSecurityLevel() == KM_SECURITY_LEVEL_SOFTWARE;
        } else {
            throw new Exception("Attestation not found " + info.getCertException());
            // throw new AttestationException(CODE_CANT_PARSE_CERT, info.getCertException());
        }
        return result;
    }
}
