/**
 * Domain events published by the Ads/Campaign Management module. Declared
 * as a {@code @NamedInterface} so other modules may depend on
 * {@code com.adren.travel.ads.event} specifically (to register
 * {@code @EventListener} methods) without gaining access to the rest of
 * the module's internals — same shape as {@code booking.event}.
 */
@org.springframework.modulith.NamedInterface("event")
package com.adren.travel.ads.event;
