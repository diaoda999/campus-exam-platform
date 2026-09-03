package com.campus.exam.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具：负责登录态长 Token 的签发与解析。
 * 一次性考试 Token 不走 JWT，而是随机 UUID + Redis 绑定（见 ExamService）。
 */
public final class JwtUtils {

    /** 默认 256bit 以上密钥，生产环境应通过配置覆盖 */
    public static final String DEFAULT_SECRET = "campus-exam-platform-jwt-secret-key-2026-please-change-in-prod";

    /** 默认有效期 8 小时 */
    public static final long DEFAULT_TTL_MILLIS = 8 * 60 * 60 * 1000L;

    private JwtUtils() {
    }

    private static SecretKey buildKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String generate(Long userId, String username, String role, String secret, Long ttlMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("username", username);
        claims.put("role", role);
        long ttl = ttlMillis == null ? DEFAULT_TTL_MILLIS : ttlMillis;
        Date now = new Date();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttl))
                .signWith(buildKey(secret))
                .compact();
    }

    public static Claims parse(String token, String secret) {
        return Jwts.parser()
                .verifyWith(buildKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Long getUserId(Claims claims) {
        Object uid = claims.get("uid");
        if (uid instanceof Number number) {
            return number.longValue();
        }
        return uid == null ? null : Long.valueOf(uid.toString());
    }
}
