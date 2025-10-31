package com.example.bankcards.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Date;

@Component
public class JWTUtil {
    @Value("${jwt_secret}")
    private String secret;

    @Value("${app.security.jwt.expiration}")
    private java.time.Duration expiration;

    public String generateAccessToken(Long id, String email, String role) {
        var now = java.time.Instant.now();
        var exp = now.plus(expiration);

        return JWT.create()
                .withSubject("Owner details")
                .withClaim("id", id)
                .withClaim("email", email)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withIssuer("ADMIN")
                .withExpiresAt(java.util.Date.from(exp))
                .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT validateAccessToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .withSubject("Owner details")
                .withIssuer("ADMIN")
                .build();
        return verifier.verify(token);
    }
}
