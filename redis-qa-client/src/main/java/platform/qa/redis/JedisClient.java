package platform.qa.redis;

import platform.qa.entities.Redis;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JedisClient {
    private final Jedis jedis;

    public JedisClient(Redis redis) {
        jedis = new Jedis(URI.create(redis.getUrl()), 10000);
        jedis.auth(redis.getPassword());
    }

    public JedisClient(List<Redis> redisList) {
        jedis = getRedisMaster(redisList);
    }

    public void set(String key, String value) {
        jedis.set(key, value);
    }

    public String get(String key) {
        return jedis.get(key);
    }

    public String hget(String key, String field) {
        return jedis.hget(key, field);
    }

    public long hset(String key, String field, String value) {
        return jedis.hset(key, field, value);
    }

    public long hset(String key, Map<String, String> hash) {
        return jedis.hset(key, hash);
    }

    public Set<String> getAllKeys() {
        return jedis.keys("*");
    }

    public Set<String> getKeys(String pattern) {
        return jedis.keys(pattern);
    }

    public void append(String key, String value) {
        jedis.append(key, value);
    }

    public void copy(String sourceKey, String targetKey, boolean isReplaceNeeded) {
        jedis.copy(sourceKey, targetKey, isReplaceNeeded);
    }

    public void delete(String key) {
        jedis.del(key);
    }

    public boolean isKeyPresent(String key) {
        return jedis.get(key) != null;
    }

    public void setIfNotPresent(String key, String value) {
        if (!isKeyPresent(key)) {
            set(key, value);
        }
    }

    public void deleteIfPresent(String key) {
        if (isKeyPresent(key)) {
            delete(key);
        }
    }

    public void close() {
        jedis.close();
    }

    public static boolean isMaster(Jedis jedis) {
        String info = jedis.info("Replication");
        return info.contains("role:master");
    }

    public Jedis getRedisMaster(List<Redis> redisList) {
        for (Redis redis : redisList) {
            Jedis jedis = new Jedis(URI.create(redis.getUrl()), 10000);
            jedis.auth(redis.getPassword());

            if (isMaster(jedis)) {
                return jedis;
            }

            jedis.close();
        }

        throw new IllegalStateException("Redis master not found");
    }
}
