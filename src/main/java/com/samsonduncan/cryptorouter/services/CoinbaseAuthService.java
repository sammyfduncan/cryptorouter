package com.samsonduncan.cryptorouter.services;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.PrivateKey;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class CoinbaseAuthService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Value("${coinbase.advanced.api.key.name}")
    private String apiKeyName;

    // inject the key as a single line string from application.properties
    @Value("${coinbase.advanced.api.private.key}")
    private String privateKeyString;

    public String generateJwt() {
        try {
            //Uses the official SDK's method.
            PrivateKey privateKey = parsePrivateKeyFromString(privateKeyString);
            String jwtId = UUID.randomUUID().toString();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .keyID(apiKeyName)
                    .customParam("nonce", UUID.randomUUID().toString())
                    .build();

            // uri for WebSocket
            String uri = "GET https://api.coinbase.com/api/v3/brokerage/products";

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(apiKeyName)
                    .issuer("cdp")
                    .claim("uri", uri)
                    .expirationTime(new Date(Instant.now().plusSeconds(120).toEpochMilli()))
                    .issueTime(new Date())
                    .notBeforeTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new ECDSASigner((ECPrivateKey) privateKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Fatal error during JWT generation. Please verify the key content in application.properties.", e);
        }
    }

    private PrivateKey parsePrivateKeyFromString(String key) throws IOException {

        String sanitizedKey = key.replace("\\n", "\n");

        try (PEMParser pemParser = new PEMParser(new StringReader(sanitizedKey))) {
            Object parsedObject = pemParser.readObject();

            if (parsedObject == null) {
                throw new IOException("PEMParser returned null. Ensure the key in application.properties is a valid, single-line string with \\n separators.");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            PEMKeyPair keyPair = (PEMKeyPair) parsedObject;
            return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
        }
    }
}
