package com.sfood.automation.goods.infrastructure.external;

import java.time.Duration;

@FunctionalInterface
public interface Sleeper {
    void sleep(Duration duration);
}
