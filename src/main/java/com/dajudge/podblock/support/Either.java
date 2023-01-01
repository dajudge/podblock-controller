package com.dajudge.podblock.support;

import io.micrometer.common.lang.NonNull;

import java.util.Optional;

public class Either<A, B> {
    private final A a;
    private final B b;

    private Either(final A a, final B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Either<A, B> a(final @NonNull A a) {
        return new Either<>(a, null);
    }

    public static <A, B> Either<A, B> b(final @NonNull B b) {
        return new Either<>(null, b);
    }

    public Optional<A> a() {
        return Optional.ofNullable(a);
    }

    public Optional<B> b() {
        return Optional.ofNullable(b);
    }
}
