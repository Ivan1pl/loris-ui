package com.ivan1pl.lorisui.api.core;

/**
 * A function of two {@code int} arguments. Panels use it for content computed from the screen width and height.
 *
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface IntBiFunction<R> {
    /**
     * Applies this function to the given arguments.
     *
     * @param a the first argument
     * @param b the second argument
     * @return the function result
     */
    R apply(int a, int b);
}
