package com.dajudge.podblock.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Collections.emptyMap;

@Singleton
public class ConfigResolver {

    private List<PodBlockConfig> configs;

    @Inject
    public void ConfigResolver(@ConfigProperty(name = "podblock.config") final String podBlockConfigs) throws JsonProcessingException {
        this.configs = new ObjectMapper().readValue(podBlockConfigs, new TypeReference<>() {
        });
    }

    public Optional<PodBlockConfig> resolve(final HasMetadata object) {
        return configs.stream().filter(matches(object)).findAny();
    }

    private Predicate<? super PodBlockConfig> matches(final HasMetadata object) {
        return config -> {
            final Map<String, String> existingLabels = Optional.ofNullable(object.getMetadata().getLabels()).orElse(emptyMap());
            return existingLabels.entrySet().containsAll(config.getMatchLabels().entrySet());
        };
    }
}
