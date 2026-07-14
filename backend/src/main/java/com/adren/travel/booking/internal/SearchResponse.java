package com.adren.travel.booking.internal;

import java.util.List;

record SearchResponse(List<GeocodedLocation> locations) {
}
