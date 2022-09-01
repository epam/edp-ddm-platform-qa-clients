package platform.qa.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

public class JedisClient {
    private final Jedis jedis;

    public JedisClient(JedisService service) {
        JedisSentinelPool sentinelPool = new JedisSentinelPool(service.getMasterName(), service.getSentinels());
        jedis = sentinelPool.getResource();

        jedis.auth(service.getPassword());
    }

    public void set(String key, String value) {
        jedis.set(key, value);
    }

    public String get(String key) {
        return jedis.get(key);
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
        if (!isKeyPresent(key)){
            set(key, value);
        }
    }

    public void deleteIfPresent(String key) {
        if (isKeyPresent(key)) {
            delete(key);
        }
    }

}
