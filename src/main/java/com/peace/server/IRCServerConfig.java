package com.peace.server;

public class IRCServerConfig {
    private final String host;
    private final int port;
    private final String password;

    private final float timeoutSeconds;
    private final long chatCooldownMillis;
    private final double positionUpdateDistance;

    private IRCServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.password = builder.password;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.chatCooldownMillis = builder.chatCooldownMillis;
        this.positionUpdateDistance = builder.positionUpdateDistance;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getPassword() { return password; }
    public float getTimeoutSeconds() { return timeoutSeconds; }
    public long getChatCooldownMillis() { return chatCooldownMillis; }
    public double getPositionUpdateDistance() { return positionUpdateDistance; }

    public static class Builder {
        private String host;
        private int port;
        private String password;

        private float timeoutSeconds = 5f;
        private long chatCooldownMillis = 500;
        private double positionUpdateDistance = 3;

        public Builder() {
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder timeoutSeconds(float timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder chatCooldownMillis(long chatCooldownMillis) {
            this.chatCooldownMillis = chatCooldownMillis;
            return this;
        }

        public Builder positionUpdateDistance(double positionUpdateDistance) {
            this.positionUpdateDistance = positionUpdateDistance;
            return this;
        }

        public IRCServerConfig build() {
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalStateException("Host must be provided");
            }
            if (port < 1 || port > 65535) {
                throw new IllegalStateException("Port must be between 1 and 65535");
            }
            if (password == null || password.trim().isEmpty()) {
                throw new IllegalStateException("Password must be supplied and not empty");
            }
            if (timeoutSeconds <= 0) {
                throw new IllegalStateException("timeoutSeconds must be >0");
            }
            if (chatCooldownMillis <= 0) {
                throw new IllegalStateException("chatCooldownMillis must be >0");
            }
            if (positionUpdateDistance <= 0) {
                throw new IllegalStateException("timeoutSeconds must be >0");
            }

            return new IRCServerConfig(this);
        }
    }
}
