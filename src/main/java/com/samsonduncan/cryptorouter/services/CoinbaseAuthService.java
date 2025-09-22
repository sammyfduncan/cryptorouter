package com.samsonduncan.cryptorouter.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

//Manages coinbase API Authentication
@Service
public class CoinbaseAuthService {

    //register bouncy castle as security provider
    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    @Value("${coinbase.advanced.api.key.name}")
    private String name;

    @Value("${coinbase.advanced.api.private.key}")
    private String key;

    //generates JWT for authenticating w/ Coinbase advanced API
    public String generateJwt() {
        try {

            PrivateKey privateKey = parsePrivateKeyWithBouncyCastle(key);
            String jwtId = UUID.randomUUID().toString();

            return Jwts.builder()
                    .setId(jwtId)
                    .setSubject(name)
                    .setIssuer(name)
                    .setExpiration(new Date(System.currentTimeMillis() + 1000 * 120))
                    .setNotBefore(new Date(System.currentTimeMillis()))
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .claim("uri", "websocket:/users/self/verify")
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();
        } catch (IOException e) {
            //wrap the exception to avoid having to declare it everywhere
            throw new RuntimeException("Failed to parse private key", e);
        }
    }

    /**
     *helper to parse a PEM key using Bouncy Castle
     */
    private PrivateKey parsePrivateKeyWithBouncyCastle(String pemKey) throws IOException {
        // use PEMParser from Bouncy Castle to read the key
        try (PEMParser pemParser = new PEMParser(new StringReader(pemKey))) {
            Object parsedObject = pemParser.readObject();

            // the JcaPEMKeyConverter converts the Bouncy Castle object to a standard Java PrivateKey
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            //object could be of different types, we get the KeyPair and extract the private key
            PEMKeyPair keyPair = (PEMKeyPair) parsedObject;
            return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
