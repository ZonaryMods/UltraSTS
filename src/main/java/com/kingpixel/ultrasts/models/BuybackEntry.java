package com.kingpixel.ultrasts.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.ultrasts.UltraSTS;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuybackEntry {
  @Builder.Default
  private UUID id = UUID.randomUUID();
  private Pokemon pokemon;
  private BigDecimal originalPrice;
  private String stsId;
  private EconomyUse economy;
  @Builder.Default
  private long timestamp = System.currentTimeMillis();

  /**
   * Checks if this buyback entry has expired based on maximum allowed duration.
   *
   * @param maxAgeMillis Max duration in milliseconds.
   * @return true if expired.
   */
  public boolean isExpired(long maxAgeMillis) {
    if (maxAgeMillis <= 0) return false;
    return (System.currentTimeMillis() - timestamp) > maxAgeMillis;
  }

  /**
   * Returns human-readable remaining time before this Pokémon expires.
   *
   * @param maxAgeMillis Max duration in milliseconds.
   * @return Formatted remaining time string.
   */
  public String getRemainingTime(long maxAgeMillis) {
    if (maxAgeMillis <= 0) {
      return UltraSTS.lang != null && UltraSTS.lang.getBuybackPermanent() != null
        ? UltraSTS.lang.getBuybackPermanent()
        : "Permanent";
    }
    long expiresAt = timestamp + maxAgeMillis;
    return PlayerUtils.getCooldown(expiresAt);
  }
}
