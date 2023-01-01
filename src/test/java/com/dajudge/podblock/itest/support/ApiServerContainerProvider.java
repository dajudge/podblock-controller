package com.dajudge.podblock.itest.support;

import com.dajudge.kindcontainer.ApiServerContainer;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import static io.fabric8.kubernetes.client.Config.fromKubeconfig;

@Alternative
public class ApiServerContainerProvider {
    @Produces
    @Singleton
    public KubernetesClient kubernetesClient(@ConfigProperty(name = "podblock.kubeconfig") String kubeconfig) {
        return new KubernetesClientBuilder()
                .withConfig(fromKubeconfig(kubeconfig))
                .build();
    }
}
