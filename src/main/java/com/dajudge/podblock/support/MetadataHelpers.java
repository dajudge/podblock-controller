package com.dajudge.podblock.support;

import io.fabric8.kubernetes.api.model.HasMetadata;

public final class MetadataHelpers {
    private MetadataHelpers() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String displayName(final HasMetadata object) {
        return "%s:%s/%s".formatted(object.getKind(), object.getMetadata().getNamespace(), object.getMetadata().getName());
    }
}
