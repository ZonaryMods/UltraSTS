package com.kingpixel.ultrasts.api.model;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Headless view model representing a Pokémon available for buyback recovery.
 */
public class StsBuybackEntryView {

  private final UUID entryId;
  private final Pokemon pokemon;
  private final String pokemonName;
  private final int pokemonLevel;
  private final boolean shiny;
  private final String stsId;
  private final BigDecimal originalPrice;
  private final BigDecimal buybackPrice;
  private final String formattedBuybackPrice;
  private final long timestamp;
  private final long expiresAt;
  private final long remainingMillis;
  private final String formattedRemaining;
  private final boolean expired;

  public StsBuybackEntryView(UUID entryId,
                             Pokemon pokemon,
                             String pokemonName,
                             int pokemonLevel,
                             boolean shiny,
                             String stsId,
                             BigDecimal originalPrice,
                             BigDecimal buybackPrice,
                             String formattedBuybackPrice,
                             long timestamp,
                             long expiresAt,
                             long remainingMillis,
                             String formattedRemaining,
                             boolean expired) {
    this.entryId = entryId;
    this.pokemon = pokemon;
    this.pokemonName = pokemonName != null ? pokemonName : "";
    this.pokemonLevel = pokemonLevel;
    this.shiny = shiny;
    this.stsId = stsId != null ? stsId : "";
    this.originalPrice = originalPrice != null ? originalPrice : BigDecimal.ZERO;
    this.buybackPrice = buybackPrice != null ? buybackPrice : BigDecimal.ZERO;
    this.formattedBuybackPrice = formattedBuybackPrice != null ? formattedBuybackPrice : "";
    this.timestamp = timestamp;
    this.expiresAt = expiresAt;
    this.remainingMillis = Math.max(0L, remainingMillis);
    this.formattedRemaining = formattedRemaining != null ? formattedRemaining : "";
    this.expired = expired;
  }

  public UUID getEntryId() {
    return entryId;
  }

  public Pokemon getPokemon() {
    return pokemon;
  }

  public String getPokemonName() {
    return pokemonName;
  }

  public int getPokemonLevel() {
    return pokemonLevel;
  }

  public boolean isShiny() {
    return shiny;
  }

  public String getStsId() {
    return stsId;
  }

  public BigDecimal getOriginalPrice() {
    return originalPrice;
  }

  public BigDecimal getBuybackPrice() {
    return buybackPrice;
  }

  public String getFormattedBuybackPrice() {
    return formattedBuybackPrice;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getExpiresAt() {
    return expiresAt;
  }

  public long getRemainingMillis() {
    return remainingMillis;
  }

  public String getFormattedRemaining() {
    return formattedRemaining;
  }

  public boolean isExpired() {
    return expired;
  }

  @Override
  public String toString() {
    return "StsBuybackEntryView{" +
      "entryId=" + entryId +
      ", pokemonName='" + pokemonName + '\'' +
      ", stsId='" + stsId + '\'' +
      ", buybackPrice=" + buybackPrice +
      ", expired=" + expired +
      '}';
  }
}
