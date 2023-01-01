package com.dajudge.podblock.itest.support;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

public class ApiServerResource implements QuarkusTestResourceLifecycleManager {
    private static final ApiServerContainer<?> K8S = new ApiServerContainer<>().withAdmissionController(admissions -> {
        admissions.mutating()
                .withNewWebhook("add-finalizer.podblock.dajudge.com")
                .atPort(8081)
                .withPath("/webhooks/pods/mutate")
                .withNewRule()
                .withApiGroups("")
                .withApiVersions("v1")
                .withResources("pods")
                .withScope("Namespaced")
                .withOperations("CREATE")
                .endRule()
                .endWebhook()
                .build();
    });

    @Override
    public Map<String, String> start() {
        K8S.start();
        return Map.of("podblock.kubeconfig", K8S.getKubeconfig());
    }

    @Override
    public void stop() {
        K8S.stop();
    }
}
