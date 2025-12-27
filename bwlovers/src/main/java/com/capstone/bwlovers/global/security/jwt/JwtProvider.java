package com.capstone.bwlovers.global.security.jwt;

import com.capstone.bwlovers.global.exception.CustomException;
import com.capstone.bwlovers.global.exception.ExceptionCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expire-ms}")
    private long accessExpireMs;

    @Value("${jwt.refresh-token-expire-ms}")
    private long refreshExpireMs;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String subject, List<String> roles) {
        return createToken(subject, roles, accessExpireMs, "access");
    }

    public String createRefreshToken(String subject, List<String> roles) {
        return createToken(subject, roles, refreshExpireMs, "refresh");
    }

    private String createToken(String subject, List<String> roles, long expireMs, String type) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expireMs);

        return Jwts.builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("typ", type)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 유효성만 boolean으로 확인하고 싶을 때
     */
    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (CustomException e) {
            return false;
        }
    }

    /**
     * 토큰 검증 + 에러를 ExceptionCode로 변환해서 던짐
     */
    public void validateOrThrow(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_EMPTY);
        }
        parseClaims(token); // 여기서 expired/invalid를 CustomException으로 던짐
    }

    /**
     * Claims 파싱 (여기서 expired/invalid를 의미 있는 코드로 매핑)
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ExceptionCode.AUTH_TOKEN_INVALID);
        }
    }

    public boolean isRefreshToken(String token) {
        Claims claims = parseClaims(token);
        String typ = claims.get("typ", String.class);
        return "refresh".equals(typ);
    }


    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        List<SimpleGrantedAuthority> authorities = (roles == null)
                ? List.of()
                : roles.stream().map(SimpleGrantedAuthority::new).toList();

        return new UsernamePasswordAuthenticationToken(subject, null, authorities);
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }
}
