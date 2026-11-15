package com.wagba.restaurants.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PublicKeyLoader {

    private PublicKeyLoader() {
    }

    static RSAPublicKey load(Path path) {
        if (!Files.exists(path)) {
            throw new IllegalStateException("JWT public key not found at " + path.toAbsolutePath()
                    + ". Start users-service once so it generates keys/, and check that the run "
                    + "configuration's working directory is the repository root.");
        }
        try {
            StringBuilder base64 = new StringBuilder();
            for (String line : Files.readAllLines(path, StandardCharsets.US_ASCII)) {
                if (!line.startsWith("-----")) {
                    base64.append(line.trim());
                }
            }
            byte[] der = Base64.getDecoder().decode(base64.toString());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Cannot read the JWT public key at " + path, e);
        }
    }
}