package com.ivan1pl.lorisui.api.core;

@FunctionalInterface
public interface IntBiFunction<R> {
    R apply(int a, int b);
}
