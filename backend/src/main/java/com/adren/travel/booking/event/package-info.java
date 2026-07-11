/**
 * Domain events published by the Booking module. Declared as a
 * {@code @NamedInterface} so other modules may depend on
 * {@code com.adren.travel.booking.event} specifically (to register
 * {@code @EventListener} methods) without gaining access to the rest of the
 * module's internals.
 */
@org.springframework.modulith.NamedInterface("event")
package com.adren.travel.booking.event;
