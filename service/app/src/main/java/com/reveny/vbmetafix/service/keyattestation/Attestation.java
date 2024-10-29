package com.reveny.vbmetafix.service.keyattestation;

import android.os.Build;
import android.util.Log;

import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Attestation {
    static final String EAT_OID = "1.3.6.1.4.1.11129.2.1.25";
    static final String ASN1_OID = "1.3.6.1.4.1.11129.2.1.17";
    static final String KNOX_OID = "1.3.6.1.4.1.236.11.3.23.7";
    static final String KEY_USAGE_OID = "2.5.29.15"; // Standard key usage extension.
    static final String CRL_DP_OID = "2.5.29.31"; // Standard CRL Distribution Points extension.

    public static final int KM_SECURITY_LEVEL_SOFTWARE = 0;
    public static final int KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 1;
    public static final int KM_SECURITY_LEVEL_STRONG_BOX = 2;

    int attestationVersion;
    int keymasterVersion;
    int keymasterSecurityLevel;
    byte[] attestationChallenge;
    byte[] uniqueId;
    AuthorizationList softwareEnforced;
    AuthorizationList teeEnforced;
    Set<String> unexpectedExtensionOids;

    public static Attestation loadFromCertificate(X509Certificate x509Cert) throws CertificateParsingException {
        if (x509Cert.getExtensionValue(ASN1_OID) == null) {
            throw new CertificateParsingException("No attestation extensions found");
        }

        if (x509Cert.getExtensionValue(CRL_DP_OID) != null) {
            Log.w("Attestation", "CRL Distribution Points extension found in leaf certificate.");
        }

        return new ASN1Attestation(x509Cert);
    }

    Attestation(X509Certificate x509Cert) {
        unexpectedExtensionOids = retrieveUnexpectedExtensionOids(x509Cert);
    }

    public static String securityLevelToString(int attestationSecurityLevel) {
        switch (attestationSecurityLevel) {
            case KM_SECURITY_LEVEL_SOFTWARE:
                return "Software";
            case KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT:
                return "TEE";
            case KM_SECURITY_LEVEL_STRONG_BOX:
                return "StrongBox";
            default:
                return "Unknown (" + attestationSecurityLevel + ")";
        }
    }

    public static String attestationVersionToString(int version) {
        switch (version) {
            case 1:
                return "Keymaster 2.0";
            case 2:
                return "Keymaster 3.0";
            case 3:
                return "Keymaster 4.0";
            case 4:
                return "Keymaster 4.1";
            case 100:
                return "KeyMint 1.0";
            case 200:
                return "KeyMint 2.0";
            case 300:
                return "KeyMint 3.0";
            default:
                return "Unknown (" + version + ")";
        }
    }

    public static String keymasterVersionToString(int version) {
        switch (version) {
            case 0:
                return "Keymaster 0.2 or 0.3";
            case 1:
                return "Keymaster 1.0";
            case 2:
                return "Keymaster 2.0";
            case 3:
                return "Keymaster 3.0";
            case 4:
                return "Keymaster 4.0";
            case 41:
                return "Keymaster 4.1";
            case 100:
                return "KeyMint 1.0";
            case 200:
                return "KeyMint 2.0";
            case 300:
                return "KeyMint 3.0";
            default:
                return "Unknown (" + version + ")";
        }
    }

    public int getAttestationVersion() {
        return attestationVersion;
    }

    public abstract int getAttestationSecurityLevel();

    public abstract RootOfTrust getRootOfTrust();

    // Returns one of the KM_VERSION_* values define above.
    public int getKeymasterVersion() {
        return keymasterVersion;
    }

    public int getKeymasterSecurityLevel() {
        return keymasterSecurityLevel;
    }

    public byte[] getAttestationChallenge() {
        return attestationChallenge;
    }

    public byte[] getUniqueId() {
        return uniqueId;
    }

    public AuthorizationList getSoftwareEnforced() {
        return softwareEnforced;
    }

    public AuthorizationList getTeeEnforced() {
        return teeEnforced;
    }

    public Set<String> getUnexpectedExtensionOids() {
        return unexpectedExtensionOids;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("Extension type: " + getClass());
        s.append("\nAttest version: " + attestationVersionToString(attestationVersion));
        s.append("\nAttest security: " + securityLevelToString(getAttestationSecurityLevel()));
        s.append("\nKM version: " + keymasterVersionToString(keymasterVersion));
        s.append("\nKM security: " + securityLevelToString(keymasterSecurityLevel));

        s.append("\n-- SW enforced --");
        s.append(softwareEnforced);
        s.append("\n-- TEE enforced --");
        s.append(teeEnforced);

        return s.toString();
    }

    Set<String> retrieveUnexpectedExtensionOids(X509Certificate x509Cert) {
        Set<String> extensionOIDs = new HashSet<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Add critical extension OIDs excluding KEY_USAGE_OID
            extensionOIDs.addAll(x509Cert.getCriticalExtensionOIDs()
                .stream()
                .filter(s -> !KEY_USAGE_OID.equals(s))
                .collect(Collectors.toList()));

            // Add non-critical extension OIDs excluding ASN1_OID and EAT_OID
            extensionOIDs.addAll(x509Cert.getNonCriticalExtensionOIDs()
                .stream()
                .filter(s -> !ASN1_OID.equals(s) && !EAT_OID.equals(s))
                .collect(Collectors.toList()));
        }

        return extensionOIDs;
    }
}
