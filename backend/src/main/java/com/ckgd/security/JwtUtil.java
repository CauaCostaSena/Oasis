package com.ckgd.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${ckgd.jwt.secret}") String secret,
                   @Value("${ckgd.jwt.expiration-ms:86400000}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Defina CKGD_JWT_SECRET com pelo menos 32 bytes aleatorios.");
        }
        if (expirationMs <= 0) throw new IllegalStateException("A expiracao JWT deve ser positiva.");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(String cnpj, String tipo) {
        if (!"EMPRESA".equals(tipo) || cnpj == null || !cnpj.matches("[0-9]{14}")) {
            throw new IllegalArgumentException("Somente empresas podem autenticar.");
        }
        Date agora = new Date();
        return Jwts.builder().subject(cnpj).claim("tipo", "EMPRESA")
                .issuedAt(agora).expiration(new Date(agora.getTime() + expirationMs))
                .signWith(signingKey).compact();
    }

    /** Valida assinatura, prazo, tipo e subject em uma unica leitura. */
    public Optional<String> autenticarEmpresa(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            String subject = claims.getSubject();
            if (!"EMPRESA".equals(claims.get("tipo", String.class)) || claims.getExpiration() == null
                    || subject == null || !subject.matches("[0-9]{14}")) return Optional.empty();
            return Optional.of(subject);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
