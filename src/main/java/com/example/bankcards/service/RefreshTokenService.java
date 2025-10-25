package com.example.bankcards.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {
    private final RedisTemplate<String, String> redisTemplate;
    private final static long REFRESH_TOKEN_EXPIRATION_MINUTES = 60 * 24 * 7;

    public RefreshTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveRefreshToken(String username, String refreshToken) {
        redisTemplate.opsForValue()
                .set(username, refreshToken, REFRESH_TOKEN_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    public String getRefreshToken(String username) {
        return redisTemplate.opsForValue().get(username);
    }

    public void deleteRefreshToken(String username) {
        redisTemplate.delete(username);
    }
}
