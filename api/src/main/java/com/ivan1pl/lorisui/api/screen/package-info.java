/**
 * Provides {@link com.ivan1pl.lorisui.api.screen.LorisScreen}, the base class for application screens.
 * <p>
 * A screen builds its component tree asynchronously, keeps drawing the most recent tree each time the host renders,
 * and receives lifecycle callbacks when the host starts or stops displaying it.
 * <p>
 * {@link com.ivan1pl.lorisui.api.screen.ScreenManager} keeps a host's named screens, tracks the current one and fires
 * the lifecycle callbacks, pairing every completed start with at most one end.
 * {@link com.ivan1pl.lorisui.api.screen.DeferredRelease} lets a host defer its release until the frame, screen switch
 * or callback that requested it has finished, and run multi-step releases without losing a failure.
 */
package com.ivan1pl.lorisui.api.screen;
