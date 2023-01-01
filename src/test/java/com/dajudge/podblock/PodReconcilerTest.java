package com.dajudge.podblock;

import com.dajudge.podblock.reconciler.PodReconciler;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.dajudge.podblock.support.MetadataTestHelpers.addAnnotation;
import static com.dajudge.podblock.support.MetadataTestHelpers.removeAnnotation;
import static com.dajudge.podblock.webhook.MutatingPodWebhook.FINALIZER_NAME;
import static com.dajudge.podblock.webhook.MutatingPodWebhook.MIN_AGE_SECS_ANNOTATION_NAME;
import static java.time.Instant.EPOCH;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PodReconcilerTest {
    private static final Instant START_TIME = EPOCH.plus(Duration.of(1, MINUTES));
    private static final int MIN_AGE_SECS = 42;
    private static final Instant AFTER_TIMEOUT = START_TIME.plus(Duration.of(MIN_AGE_SECS + 1, SECONDS));
    private static final Instant BEFORE_TIMEOUT = START_TIME.plus(Duration.of(MIN_AGE_SECS - 1, SECONDS));
    private final Clock clock = mock(Clock.class);
    private PodReconciler reconciler;

    @BeforeEach
    public void beforeEach() {
        reconciler = new PodReconciler(clock);
    }

    @Test
    public void ignores_pod_without_annotation() {
        // GIVEN
        final Pod pod = addAnnotation(newTestPod(START_TIME), MIN_AGE_SECS_ANNOTATION_NAME, "lolcats");
        when(clock.instant()).thenReturn(BEFORE_TIMEOUT);

        // WHEN
        final DeleteControl result = reconciler.cleanup(pod, getPodContext());

        // THEN
        assertTrue(result.isRemoveFinalizer());
        assertFalse(result.getScheduleDelay().isPresent());
    }

    @Test
    public void ignores_pod_without_parseable_annotation() {
        // GIVEN
        final Pod pod = removeAnnotation(newTestPod(START_TIME), MIN_AGE_SECS_ANNOTATION_NAME);
        when(clock.instant()).thenReturn(BEFORE_TIMEOUT);

        // WHEN
        final DeleteControl result = reconciler.cleanup(pod, getPodContext());

        // THEN
        assertTrue(result.isRemoveFinalizer());
        assertFalse(result.getScheduleDelay().isPresent());
    }

    @Test
    public void retains_finalizer_before_timeout() {
        // GIVEN
        final Pod pod = newTestPod(START_TIME);
        when(clock.instant()).thenReturn(BEFORE_TIMEOUT);

        // WHEN
        final DeleteControl result = reconciler.cleanup(pod, getPodContext());

        // THEN
        assertFalse(result.isRemoveFinalizer());
        assertEquals(1000, result.getScheduleDelay().orElseThrow(() -> new AssertionError("No retry scheduled")));
    }


    @Test
    public void removes_finalizer_after_timeout() {
        // GIVEN
        final Pod pod = newTestPod(START_TIME);
        when(clock.instant()).thenReturn(AFTER_TIMEOUT);

        // WHEN
        final DeleteControl result = reconciler.cleanup(pod, getPodContext());

        // THEN
        assertTrue(result.isRemoveFinalizer());
        assertFalse(result.getScheduleDelay().isPresent());
    }

    private Pod newTestPod(final Instant creationTimestamp) {
        return new PodBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("pod-%s".formatted(UUID.randomUUID().toString()))
                        .withNamespace("ns-%s".formatted(UUID.randomUUID().toString()))
                        .withFinalizers(FINALIZER_NAME)
                        .withCreationTimestamp(ISO_INSTANT.format(creationTimestamp))
                        .withDeletionTimestamp("non-null")
                        .withAnnotations(Map.of(MIN_AGE_SECS_ANNOTATION_NAME, "%d".formatted(MIN_AGE_SECS)))
                        .build())
                .build();
    }

    private static Context<Pod> getPodContext() {
        @SuppressWarnings("unchecked") final Context<Pod> context = mock(Context.class);
        return context;
    }
}
