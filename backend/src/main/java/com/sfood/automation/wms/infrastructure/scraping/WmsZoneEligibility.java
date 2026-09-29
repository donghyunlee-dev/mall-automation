package com.sfood.automation.wms.infrastructure.scraping;

import java.util.Set;

final class WmsZoneEligibility {

    private static final Set<String> WHITELIST_ZONE_CODES = Set.of("B1");

    private WmsZoneEligibility() {
    }

    static boolean isAllowed(String zoneCd) {
        if (zoneCd == null || zoneCd.isBlank()) {
            return false;
        }
        return isNumeric(zoneCd) || WHITELIST_ZONE_CODES.contains(zoneCd);
    }

    private static boolean isNumeric(String value) {
        return value.chars().allMatch(Character::isDigit);
    }
}
