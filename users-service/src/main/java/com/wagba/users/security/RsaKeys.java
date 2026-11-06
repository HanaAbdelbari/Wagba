package com.wagba.users.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaKeys {

    private RsaKeys() {
    }

    static KeyPair loadOrCreate(Path privatePath, Path publicPath, boolean generateIfMissing)
            throws GeneralSecurityException, IOException {

        if (Files.exists(privatePath) && Files.exists(publicPath)) {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(readPem(privatePath)));
            PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(readPem(publicPath)));
            return new KeyPair(pub, priv);
        }

        if (!generateIfMissing) {
            throw new IllegalStateException(
                    "JWT key files not found: " + privatePath + " / " + publicPath);
        }

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        Files.createDirectories(privatePath.toAbsolutePath().getParent());
        Files.createDirectories(publicPath.toAbsolutePath().getParent());
        writePem(privatePath, "PRIVATE KEY", pair.getPrivate().getEncoded());
        writePem(publicPath, "PUBLIC KEY", pair.getPublic().getEncoded());
        return pair;
    }

    private static byte[] readPem(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line : Files.readAllLines(path, StandardCharsets.US_ASCII)) {
            if (!line.startsWith("-----")) {
                sb.append(line.trim());
            }
        }
        return Base64.getDecoder().decode(sb.toString());
    }

    private static void writePem(Path path, String type, byte[] der) throws IOException {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                .encodeToString(der);
        String pem = "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem, StandardCharsets.US_ASCII);
    }
}