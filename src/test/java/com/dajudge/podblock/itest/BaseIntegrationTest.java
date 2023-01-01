package com.dajudge.podblock.itest;


import com.dajudge.podblock.itest.support.ApiServerResource;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;

import static java.util.UUID.randomUUID;

@QuarkusTestResource(ApiServerResource.class)
abstract class BaseIntegrationTest {

    @Inject
    KubernetesClient client;

    @Inject
    Operator operator;

    String namespace;

    @BeforeEach
    public void beforeEach() {
        namespace = client.resource(new NamespaceBuilder()
                .withNewMetadata()
                .withName("ns-%s".formatted(randomUUID()))
                .endMetadata()
                .build()).create().getMetadata().getName();
        client.resource(new ServiceAccountBuilder()
                .withNewMetadata()
                .withName("default")
                .withNamespace(namespace)
                .endMetadata()
                .build()).create();

        operator.start();
    }
}
