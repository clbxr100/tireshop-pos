package com.tireshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tiered markup pricing for tires.
 *
 * The price entered on the inventory page is the shop's COST (what we pay).
 * The customer-facing sell price is cost plus a markup that depends on the cost:
 *
 *   cost up to $100        -> +7%
 *   cost $100.01 - $300    -> +5%
 *   cost over $300         -> +3%
 */
public final class TirePricing {

    private static final BigDecimal TIER1_MAX = new BigDecimal("100");
    private static final BigDecimal TIER2_MAX = new BigDecimal("300");
    private static final BigDecimal TIER1_RATE = new BigDecimal("0.07");
    private static final BigDecimal TIER2_RATE = new BigDecimal("0.05");
    private static final BigDecimal TIER3_RATE = new BigDecimal("0.03");

    private TirePricing() {
    }

    /** The markup rate that applies to a given cost. */
    public static BigDecimal markupRate(BigDecimal cost) {
        if (cost == null || cost.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (cost.compareTo(TIER1_MAX) <= 0) {
            return TIER1_RATE;
        }
        if (cost.compareTo(TIER2_MAX) <= 0) {
            return TIER2_RATE;
        }
        return TIER3_RATE;
    }

    /** Cost plus tiered markup, rounded to cents. Never returns null. */
    public static BigDecimal calculateSellPrice(BigDecimal cost) {
        if (cost == null) {
            return BigDecimal.ZERO;
        }
        if (cost.signum() <= 0) {
            return cost.setScale(2, RoundingMode.HALF_UP);
        }
        return cost.add(cost.multiply(markupRate(cost))).setScale(2, RoundingMode.HALF_UP);
    }
}
