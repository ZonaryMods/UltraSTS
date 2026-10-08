package com.kingpixel.ultrasts.configs;

import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.api.PermissionApi;
import lombok.Data;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;

@Data
public class BuybackConfig {
  private boolean enabled = true;

  /**
   * Permission required to use recovery commands and open the recovery menu.
   * Default: "ultrasts.buyback". If empty or null, allows all users with base command permission.
   */
  private String permission = "ultrasts.buyback";

  /**
   * Price increase multiplier or percentage to buy back Pokémon.
   * Examples:
   * - 0.0 or 1.0: No increase (100% of the sold price).
   * - 1.2: 20% increase (120% of the sold price).
   * - 0.2: Also interpreted as +20% increase for convenience.
   */
  private double priceIncrease = 1.0;

  /**
   * How long sold Pokémon remain available for buyback before expiring.
   * e.g., "30d" (30 days), "7d", "1mo", "24h".
   */
  private DurationValue expiration = DurationValue.parse("30d");

  /**
   * Maximum number of sold Pokémon stored per user in history.
   * Keeps storage and memory hyper-optimized.
   */
  private int maxEntriesPerUser = 50;

  /**
   * Calculates the final buyback price for a given original sell price.
   *
   * @param originalPrice The amount originally received for selling the Pokémon.
   * @return The final buyback price to charge.
   */
  public BigDecimal calculateBuybackPrice(BigDecimal originalPrice) {
    if (originalPrice == null || originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    // 0 or 1.0 means no increase (original price)
    if (priceIncrease <= 0.0 || priceIncrease == 1.0) {
      return originalPrice;
    }
    // Fraction between 0 and 1 (e.g. 0.20 -> +20%)
    if (priceIncrease > 0.0 && priceIncrease < 1.0) {
      return originalPrice.multiply(BigDecimal.valueOf(1.0 + priceIncrease));
    }
    // Multiplier >= 1.0 (e.g. 1.20 -> 120%)
    return originalPrice.multiply(BigDecimal.valueOf(priceIncrease));
  }

  /**
   * Calculates the exploit-proof buyback price ensuring players cannot profit
   * from configuration changes, price adjustments, or STS formula alterations.
   *
   * Safeguards:
   * 1. Never charges less than the original sell price received (prevents profit from price drop).
   * 2. Never charges less than the current market value (prevents resale arbitrage loops).
   *
   * @param originalPrice The price originally received when the Pokémon was sold.
   * @param currentMarketPrice The current valuation evaluated by the active STS formula.
   * @return The secure, exploit-proof buyback price.
   */
  public BigDecimal calculateSecureBuybackPrice(BigDecimal originalPrice, BigDecimal currentMarketPrice) {
    BigDecimal safeOriginal = originalPrice != null ? originalPrice : BigDecimal.ZERO;
    BigDecimal safeMarket = currentMarketPrice != null ? currentMarketPrice : BigDecimal.ZERO;

    // Highest valuation serves as the base for multiplier calculation
    BigDecimal effectiveBase = safeOriginal.max(safeMarket);
    BigDecimal calculated = calculateBuybackPrice(effectiveBase);

    // Hard security floor: must be at least the original price and at least current market price
    return calculated.max(safeOriginal).max(safeMarket);
  }

  public boolean hasPermission(ServerPlayerEntity player) {
    if (permission == null || permission.isBlank()) return true;
    return PermissionApi.hasPermission(player, permission, 2);
  }

  public boolean hasPermission(ServerCommandSource source) {
    if (permission == null || permission.isBlank()) return true;
    return PermissionApi.hasPermission(source, permission, 2);
  }
}
