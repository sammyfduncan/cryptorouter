package com.samsonduncan.cryptorouter.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

//Manages coinbase API Authentication
@Service
public class CoinbaseAuthService {

    @Value("${coinbase.advanced.api.key.name}")
    private String name;

    @Value("${coinbase.advanced.api.private.key}")
    private String key;

    //generates JWT for authenticating w/ Coinbase advanced API
    public String generateJwt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        PrivateKey privateKey = parsePrivateKey(key);

        //JWT unique ID
        String jwtId = UUID.randomUUID().toString();

        //use jjwt lib to build the token
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
    }

    //helper for parsing PEM-formatted EC private key str
    private PrivateKey parsePrivateKey(String key) throws InvalidKeySpecException, NoSuchAlgorithmException {
        //remove PEM headers, footers, line breaks
        String privateKeyPem = key
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replaceAll("\\R", "") // \\R matches any line break
                .replace("-----END EC PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPem);

        //generate PrivateKey object
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

        return keyFactory.generatePrivate(keySpec);
    }
}
