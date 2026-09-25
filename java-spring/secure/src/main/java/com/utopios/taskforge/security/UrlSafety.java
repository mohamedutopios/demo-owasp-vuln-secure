package com.utopios.taskforge.security;

import java.net.InetAddress;
import java.net.URI;
import java.util.Set;

/**
 * A01 (SSRF) - n'autorise que http/https et rejette toute cible resolvant vers
 * une adresse privee, de bouclage, locale au lien, reservee ou multicast
 * (protege notamment l'acces aux metadonnees cloud et aux services internes).
 */
public final class UrlSafety {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private UrlSafety() {
    }

    public static boolean isSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        URI uri;
        try {
            uri = URI.create(raw.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            return false;
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
                        || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                        || addr.isMulticastAddress() || isUniqueLocal(addr)) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // Bloque aussi les plages IPv6 uniques locales (fc00::/7) non couvertes ci-dessus.
    private static boolean isUniqueLocal(InetAddress addr) {
        byte[] b = addr.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }
}
