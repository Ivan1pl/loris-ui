package com.ivan1pl.lorisui.api.core;

/**
 * A function of two {@code int} arguments. Panels use it for content computed from the screen width and height.
 *
 * @param <ResultType> the type of the result
 */
@FunctionalInterface
public interface IntBiFunction<ResultType> {
    /**
     * Applies this function to the given screen size.
     *
     * @param width  the screen width in logical window coordinates
     * @param height the screen height in logical window coordinates
     * @return the function result
     */
    ResultType apply(int width, int height);
}
