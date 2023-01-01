package com.dajudge.podblock;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import java.time.Clock;

@Singleton
public class ClockProvider {
    @Produces
    public Clock clock() {
        return Clock.systemUTC();
    }
}
