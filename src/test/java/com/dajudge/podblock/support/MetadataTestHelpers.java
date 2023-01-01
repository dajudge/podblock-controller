package com.dajudge.podblock.support;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.ArrayList;
import java.util.HashMap;

public final class MetadataTestHelpers {
    private MetadataTestHelpers() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T extends HasMetadata> T removeAnnotation(final T object, final String annotation) {
        if (object.getMetadata().getAnnotations() != null) {
            object.getMetadata().getAnnotations().remove(annotation);
        }
        return object;
    }

    public static <T extends HasMetadata> T removeLabels(final T object) {
        object.getMetadata().setLabels(null);
        return object;
    }

    public static <T extends HasMetadata> T addAnnotation(final T object, final String k, final String v) {
        if (object.getMetadata().getAnnotations() == null) {
            object.getMetadata().setAnnotations(new HashMap<>());
        }
        object.getMetadata().getAnnotations().put(k, v);
        return object;
    }

    public static <T extends HasMetadata> T addLabel(final T object, final String k, final String v) {
        if (object.getMetadata().getLabels() == null) {
            object.getMetadata().setLabels(new HashMap<>());
        }
        object.getMetadata().getLabels().put(k, v);
        return object;
    }

    public static <T extends HasMetadata> T addFinalizer(final T object, final String finalizer) {
        if (object.getMetadata().getFinalizers() == null) {
            object.getMetadata().setFinalizers(new ArrayList<>());
        }
        object.getMetadata().getFinalizers().add(finalizer);
        return object;
    }
}
