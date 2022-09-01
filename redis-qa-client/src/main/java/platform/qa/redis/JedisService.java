package platform.qa.redis;

import java.util.Set;

public class JedisService {
    private String host;
    private String port;
    private String password;
    private String masterName;
    private Set<String> sentinels;

    public JedisService(String host, String port, String password, String masterName, Set<String> sentinels) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.masterName = masterName;
        this.sentinels = sentinels;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public Set<String> getSentinels() {
        return sentinels;
    }

    public void setSentinels(Set<String> sentinels) {
        this.sentinels = sentinels;
    }
}
