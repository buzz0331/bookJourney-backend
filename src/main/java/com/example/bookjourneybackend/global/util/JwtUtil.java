package com.example.bookjourneybackend.global.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.NoSuchElementException;

@Slf4j
@Component
public class JwtUtil {

    private final Key key;
    private final long accessTokenExpTime;
    private final long refreshTokenExpTime;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenExpTime,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpTime
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpTime = accessTokenExpTime;
        this.refreshTokenExpTime = refreshTokenExpTime;
    }

    /**
     * Access Token 생성
     * @param userId
     * @return Access Token String
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenExpTime);
    }

    /**
     * Refresh Token 생성
     * @param userId
     * @return Refresh Token String
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenExpTime);
    }

    /**
     * JWT 생성
     * @param userId
     * @param expireTime
     * @return JWT String
     */
    private String createToken(Long userId, long expireTime) {
        Claims claims = Jwts.claims();
        claims.put("userId",userId);

        Date now = new Date();
        Date tokenValidity =new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(tokenValidity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Token에서 User ID 추출
     * @param authorization
     * @return User ID
     */
    public Long extractIdFromHeader(String authorization) {

        // Authorization 헤더에서 JWT 토큰 추출
        String jwtToken;
        try {
            jwtToken = extractJwtToken(authorization);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 헤더 포맷입니다.");
        }

        // JWT 토큰에서 사용자 정보 추출
        Long userId;
        try {
            userId = extractUserIdFromJwtToken(jwtToken);
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("토큰에서 유저 아이디를 찾을 수 없습니다.");
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 토큰 형식입니다.");
        }

        return userId;

    }

    public String extractJwtToken(String authorizationHeader) {
        String[] parts = authorizationHeader.split(" ");
        if (parts.length == 2) {
            return parts[1].trim(); // 토큰 부분 추출 및 공백 제거
        }
        throw new IllegalArgumentException("유효하지 않은 헤더 포맷입니다.");
    }

    public Long extractUserIdFromJwtToken(String jwtToken) {

        Claims claims = parseClaims(jwtToken);
        Long userId = claims.get("userId", Long.class);

        if (userId == null) {
            throw new NoSuchElementException("토큰에서 유저 아이디를 찾을 수 없습니다.");
        }
        return userId;
    }


    /**
     * JWT 검증
     * @param token
     * @return IsValidate
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.", e);
        }
        return false;
    }


    /**
     * JWT Claims 추출
     * @param token
     * @return JWT Claims
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }


    /**
     * 리프레시 토큰의 유효성을 검사
     * @param token
     * @return true/false
     */
    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("Expired Refresh Token", e);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException | UnsupportedJwtException e) {
            log.info("Invalid Refresh Token", e);
        } catch (IllegalArgumentException e) {
            log.info("Refresh JWT claims string is empty.", e);
        }
        return false;
    }

}
