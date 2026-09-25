package com.utopios.taskforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utopios.taskforge.service.Preferences;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * A08 - integrite des donnees : le cookie "prefs" est du JSON lie a une classe
 * fixe (Jackson, aucun typage polymorphe) et signe par HMAC-SHA256.
 * Toute alteration invalide la signature ; aucune desserialisation Java native.
 */
@Component
public class PrefsCodec {

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] hmacKey;
    private final Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder dec = Base64.getUrlDecoder();

    public PrefsCodec(@Value("${app.enc.password}") String secret,
                      @Value("${app.enc.salt}") String salt) {
        this.hmacKey = (secret + ":prefs:" + salt).getBytes(StandardCharsets.UTF_8);
    }

    public String encode(Preferences prefs) {
        try {
            byte[] json = mapper.writeValueAsBytes(prefs);
            String body = enc.encodeToString(json);
            String sig = enc.encodeToString(hmac(body.getBytes(StandardCharsets.UTF_8)));
            return body + "." + sig;
        } catch (Exception e) {
            throw new IllegalStateException("Encodage des preferences impossible", e);
        }
    }

    public Preferences decode(String token) {
        Preferences def = new Preferences();
        if (token == null || !token.contains(".")) {
            return def;
        }
        try {
            int dot = token.lastIndexOf('.');
            String body = token.substring(0, dot);
            String sig = token.substring(dot + 1);
            byte[] expected = hmac(body.getBytes(StandardCharsets.UTF_8));
            byte[] provided = dec.decode(sig);
            // Comparaison a temps constant : rejette toute signature invalide.
            if (!MessageDigest.isEqual(expected, provided)) {
                return def;
            }
            return mapper.readValue(dec.decode(body), Preferences.class);
        } catch (Exception e) {
            return def;
        }
    }

    private byte[] hmac(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        return mac.doFinal(data);
    }
}
