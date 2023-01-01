package com.dajudge.podblock.config;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Map;

@RegisterForReflection
public class PodBlockConfig {

    private  Map<String, String> matchLabels;
    private int minAgeSeconds;

    public int getMinAgeSeconds() {
        return minAgeSeconds;
    }

    public Map<String, String> getMatchLabels() {
        return matchLabels;
    }

    public void setMatchLabels(Map<String, String> matchLabels) {
        this.matchLabels = matchLabels;
    }

    public void setMinAgeSeconds(int minAgeSeconds) {
        this.minAgeSeconds = minAgeSeconds;
    }
}
