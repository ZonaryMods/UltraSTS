package com.kingpixel.ultrasts.api.model;

import com.cobblemon.mod.common.pokemon.Pokemon;

import java.math.BigDecimal;

/**
 * Headless view model representing a valuation quote for selling a Pokémon.
 */
public class StsPokemonQuoteView {

  private final Pokemon pokemon;
  private final String pokemonName;
  private final int pokemonLevel;
  private final boolean shiny;
  private final String stsId;
  private final BigDecimal price;
  private final String formattedPrice;
  private final boolean canSell;
  private final String blockReason;

  public StsPokemonQuoteView(Pokemon pokemon,
                             String pokemonName,
                             int pokemonLevel,
                             boolean shiny,
                             String stsId,
                             BigDecimal price,
                             String formattedPrice,
                             boolean canSell,
                             String blockReason) {
    this.pokemon = pokemon;
    this.pokemonName = pokemonName != null ? pokemonName : "";
    this.pokemonLevel = pokemonLevel;
    this.shiny = shiny;
    this.stsId = stsId != null ? stsId : "";
    this.price = price != null ? price : BigDecimal.ZERO;
    this.formattedPrice = formattedPrice != null ? formattedPrice : "";
    this.canSell = canSell;
    this.blockReason = blockReason != null ? blockReason : "";
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

  public BigDecimal getPrice() {
    return price;
  }

  public String getFormattedPrice() {
    return formattedPrice;
  }

  public boolean canSell() {
    return canSell;
  }

  public String getBlockReason() {
    return blockReason;
  }

  @Override
  public String toString() {
    return "StsPokemonQuoteView{" +
      "pokemonName='" + pokemonName + '\'' +
      ", pokemonLevel=" + pokemonLevel +
      ", shiny=" + shiny +
      ", stsId='" + stsId + '\'' +
      ", price=" + price +
      ", formattedPrice='" + formattedPrice + '\'' +
      ", canSell=" + canSell +
      ", blockReason='" + blockReason + '\'' +
      '}';
  }
}
