package com.labelhub.common.security.ratelimit;

import java.time.Duration;

public record ApiRateLimitRule(
        String method,
        String pathPattern,
        long ipRate,
        long userRate,
        Duration interval
) {

    public boolean limitsIp() {
        return ipRate > 0;
    }

    public boolean limitsUser() {
        return userRate > 0;
    }
}
