package com.dajudge.podblock.webhook;

import com.dajudge.podblock.config.ConfigResolver;
import com.dajudge.podblock.config.PodBlockConfig;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponse;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionResponseBuilder;
import io.fabric8.kubernetes.api.model.admission.v1.AdmissionReview;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;

@Path("/webhooks/pods")
public class MutatingPodWebhook {

    public static final String FINALIZER_NAME = "podblock.dajudge.com/min-age";
    public static final String MIN_AGE_SECS_ANNOTATION_NAME = "podblock.dajudge.com/min-age-seconds";
    public static final String ENABLED_LABEL_NAME = "podblock.dajudge.com/enabled";
    private final ConfigResolver configResolver;

    @Inject
    public MutatingPodWebhook(final ConfigResolver configResolver) {
        this.configResolver = configResolver;
    }

    @POST
    @Path("/mutate")
    public AdmissionReview process(final AdmissionReview review) {
        final Pod pod = (Pod) review.getRequest().getObject();
        final Optional<PodBlockConfig> config = configResolver.resolve(pod);
        review.setResponse(config.map(c -> toPatchingResponse(review, c)).orElse(untouched(review)));
        return review;
    }

    private static AdmissionResponse toPatchingResponse(final AdmissionReview review, final PodBlockConfig config) {
        final Pod pod = (Pod) review.getRequest().getObject();
        final List<String> patches = new ArrayList<>();
        addAnnotation(config.getMinAgeSeconds(), pod, patches);
        addFinalizer(pod, patches);
        addLabel(pod, patches);
        final String patch = "[%s]".formatted(join(",", patches));
        return new AdmissionResponseBuilder()
                .withAllowed(true)
                .withPatchType("JSONPatch")
                .withPatch(Base64.getEncoder().encodeToString(patch.getBytes(UTF_8)))
                .withUid(review.getRequest().getUid())
                .build();
    }

    private static void addLabel(final Pod pod, final List<String> patches) {
        if (pod.getMetadata().getLabels() == null || pod.getMetadata().getLabels().isEmpty()) {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/labels",
                        "value": {"%s": "true"}
                    }""".formatted(ENABLED_LABEL_NAME));
        } else {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/labels/podblock.dajudge.com~1enabled",
                        "value": "true"
                    }""");
        }

    }

    private static AdmissionResponse untouched(AdmissionReview review) {
        return new AdmissionResponseBuilder().withAllowed(true).withUid(review.getRequest().getUid()).build();
    }

    private static void addAnnotation(final int minAgeSeconds, final Pod pod, final List<String> patches) {
        if (pod.getMetadata().getAnnotations() == null || pod.getMetadata().getAnnotations().isEmpty()) {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/annotations",
                        "value": {"%s": "%d"}
                    }""".formatted(MIN_AGE_SECS_ANNOTATION_NAME, minAgeSeconds));
        } else {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/annotations/%s",
                        "value": "%d"
                    }""".formatted(MIN_AGE_SECS_ANNOTATION_NAME.replace("/", "~1"), minAgeSeconds));
        }
    }

    private static void addFinalizer(final Pod pod, final List<String> patches) {
        if (pod.getMetadata().getFinalizers() == null || pod.getMetadata().getFinalizers().isEmpty()) {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/finalizers",
                        "value": ["%s"]
                    }""".formatted(FINALIZER_NAME));
        } else {
            patches.add("""
                    {
                        "op": "add",
                        "path": "/metadata/finalizers/-",
                        "value": "%s"
                    }""".formatted(FINALIZER_NAME));
        }
    }
}
