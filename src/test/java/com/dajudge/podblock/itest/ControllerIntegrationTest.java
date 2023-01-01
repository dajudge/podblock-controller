package com.dajudge.podblock.itest;

import com.dajudge.podblock.itest.support.IntegrationTestProfile;
import io.fabric8.kubernetes.api.model.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.dajudge.podblock.support.MetadataTestHelpers.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;


@QuarkusTest
@TestProfile(ControllerIntegrationTest.Profile.class)
public class ControllerIntegrationTest extends BaseIntegrationTest {

    private static final int TIMEOUT = 5;

    @Test
    public void removes_finalizer_after_timeout() throws InterruptedException {
        final Pod original = newTestPod();
        final Pod pod = client.resource(original).create();
        client.resource(pod).delete();
        Thread.sleep(TIMEOUT * 1000);
        await("Pod gets removed")
                .timeout(Duration.ofSeconds(5))
                .untilAsserted(() -> assertNull(client.pods().inNamespace(namespace).withName(pod.getMetadata().getName()).get()));
    }

    @Test
    public void adds_finalizer() {
        final Pod original = newTestPod();
        final Pod pod = client.resource(original).create();
        assertTrue(pod.getMetadata().getFinalizers().contains("podblock.dajudge.com/min-age"));
    }

    @Test
    public void ignores_unmatched_pod() {
        final Pod original = removeLabels(newTestPod());
        final Pod pod = client.resource(original).create();
        assertFalse(pod.getMetadata().getFinalizers().contains("podblock.dajudge.com/min-age"));
    }

    @Test
    public void retains_existing_finalizers() {
        final Pod original = addFinalizer(newTestPod(), "test.dajudge.com/test-finalizer");
        final Pod pod = client.resource(original).create();
        assertTrue(pod.getMetadata().getFinalizers().contains("test.dajudge.com/test-finalizer"));
    }

    @Test
    public void adds_minAgeSeconds_annotation() {
        final Pod original = newTestPod();
        final Pod pod = client.resource(original).create();
        assertEquals("5", pod.getMetadata().getAnnotations().get("podblock.dajudge.com/min-age-seconds"));
    }

    @Test
    public void adds_podblock_annotation() {
        final Pod original = newTestPod();
        final Pod pod = client.resource(original).create();
        assertEquals("true", pod.getMetadata().getLabels().get("podblock.dajudge.com/enabled"));
    }

    @Test
    public void retains_existing_labels() {
        final Pod original = addLabel(newTestPod(), "test.dajudge.com/test-label", "lolcats");
        final Pod pod = client.resource(original).create();
        assertEquals("lolcats", pod.getMetadata().getLabels().get("test.dajudge.com/test-label"));
    }

    @Test
    public void retains_existing_annotations() {
        final Pod original = addAnnotation(newTestPod(), "test.dajudge.com/test-annotation", "lolcats");
        final Pod pod = client.resource(original).create();
        assertEquals("lolcats", pod.getMetadata().getAnnotations().get("test.dajudge.com/test-annotation"));
    }

    private Pod newTestPod() {
        return new PodBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withNamespace(namespace)
                        .withName("pod-%s".formatted(randomUUID().toString()))
                        .withLabels(Map.of("label", "value"))
                        .build())
                .withSpec(new PodSpecBuilder()
                        .withContainers(new ContainerBuilder()
                                .withName("conatiner")
                                .withImage("image")
                                .build())
                        .build())
                .build();
    }

    public static class Profile extends IntegrationTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            final HashMap<String, String> ret = new HashMap<>(super.getConfigOverrides());
            ret.put("podblock.config", """
                    [{
                        "matchLabels": {
                            "label": "value"
                        },
                        "minAgeSeconds": %d
                    }]""".formatted(TIMEOUT));
            return ret;
        }
    }
}