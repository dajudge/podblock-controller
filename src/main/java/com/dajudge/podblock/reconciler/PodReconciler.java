package com.dajudge.podblock.reconciler;

import com.dajudge.podblock.support.Either;
import io.fabric8.kubernetes.api.model.Pod;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.dajudge.podblock.support.MetadataHelpers.displayName;
import static com.dajudge.podblock.webhook.MutatingPodWebhook.*;
import static io.javaoperatorsdk.operator.api.reconciler.Constants.WATCH_ALL_NAMESPACES;
import static java.lang.Integer.parseInt;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

@ControllerConfiguration(
        namespaces = WATCH_ALL_NAMESPACES,
        labelSelector = ENABLED_LABEL_NAME + "=true",
        finalizerName = FINALIZER_NAME,
        generationAwareEventProcessing = false
)
public class PodReconciler implements Reconciler<Pod>, Cleaner<Pod> {
    private static final Logger LOG = LoggerFactory.getLogger(PodReconciler.class);
    private final Clock clock;

    @Inject
    public PodReconciler(final Clock clock) {
        this.clock = clock;
    }

    @Override
    public DeleteControl cleanup(final Pod pod, final Context<Pod> context) {
        LOG.debug("Handling cleanup of {}", displayName(pod));

        final String timestamp = pod.getMetadata().getCreationTimestamp();
        LOG.trace("Raw creation timestamp of {}: {}", displayName(pod), timestamp);
        final Instant parsedTimestamp = Instant.from(ISO_INSTANT.parse(timestamp));
        LOG.trace("Parsed creation timestamp of {}: {}", displayName(pod), parsedTimestamp);
        final Optional<String> annotation = Optional.ofNullable(pod.getMetadata().getAnnotations()).map(it -> it.get(MIN_AGE_SECS_ANNOTATION_NAME));
        if (annotation.isEmpty()) {
            LOG.warn("Ignoring {}: does not have annotation {}", displayName(pod), MIN_AGE_SECS_ANNOTATION_NAME);
            return DeleteControl.defaultDelete();
        }

        final String minAgeSecsRaw = pod.getMetadata().getAnnotations().get(MIN_AGE_SECS_ANNOTATION_NAME);
        LOG.trace("Raw value of annotation {} of {}: {}", MIN_AGE_SECS_ANNOTATION_NAME, displayName(pod), minAgeSecsRaw);
        final Either<Integer, Exception> minAgeSecsParseResult = safeParseInt(minAgeSecsRaw);
        if (minAgeSecsParseResult.b().isPresent()) {
            LOG.warn("Ignoring {}: failed to parse annotation value of {}", displayName(pod), MIN_AGE_SECS_ANNOTATION_NAME, minAgeSecsParseResult.b().get());
            return DeleteControl.defaultDelete();
        }

        final int minAgeSecs = minAgeSecsParseResult.a().get();
        LOG.trace("Min age in seconds for {}: {}", displayName(pod), minAgeSecs);
        final Instant timeout = parsedTimestamp.plusSeconds(minAgeSecs);
        LOG.trace("Timeout for {}: {}", displayName(pod), timeout);
        if (!timeout.isBefore(clock.instant())) {
            LOG.debug("Rescheduling {}", displayName(pod));
            return DeleteControl.noFinalizerRemoval().rescheduleAfter(Duration.ofSeconds(1));
        }

        LOG.info("Removing finalizer {} from {}", FINALIZER_NAME, displayName(pod));
        return DeleteControl.defaultDelete();
    }

    private Either<Integer, Exception> safeParseInt(final String s) {
        try {
            return Either.a(parseInt(s));
        } catch (final NumberFormatException e) {
            return Either.b(e);
        }
    }

    @Override
    public UpdateControl<Pod> reconcile(Pod resource, Context<Pod> context) {
        // We actually don't want to do anything until deletion
        return UpdateControl.noUpdate();
    }
}
