package com.dajudge.podblock.itest.support;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Set;

public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(ApiServerContainerProvider.class);
    }

}
