package com.hiepnn.mini_messeger.service;

import com.hiepnn.mini_messeger.config.RedisConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class PresenceService {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String PRESENCE_KEY_PREFIX = "user:presence:";
    private static final String ROUTING_KEY = "message:routing";

    public PresenceService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setOnline(Long userId) {
        // Set presence to online with 5 minutes TTL
        redisTemplate.opsForValue().set(PRESENCE_KEY_PREFIX + userId, "online", 5, TimeUnit.MINUTES);
        // Register server routing
        redisTemplate.opsForHash().put(ROUTING_KEY, userId.toString(), RedisConfig.getServerId());
    }

    public void setOffline(Long userId) {
        redisTemplate.delete(PRESENCE_KEY_PREFIX + userId);
        redisTemplate.opsForHash().delete(ROUTING_KEY, userId.toString());
    }

    public String getUserPresence(Long userId) {
        Object presence = redisTemplate.opsForValue().get(PRESENCE_KEY_PREFIX + userId);
        return presence != null ? presence.toString() : "offline";
    }

    public String getUserServer(Long userId) {
        Object serverId = redisTemplate.opsForHash().get(ROUTING_KEY, userId.toString());
        return serverId != null ? serverId.toString() : null;
    }
}
