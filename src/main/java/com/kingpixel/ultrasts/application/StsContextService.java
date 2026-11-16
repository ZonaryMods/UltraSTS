package com.kingpixel.ultrasts.application;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.api.model.PlayerStsContext;
import com.kingpixel.ultrasts.api.model.StsBuybackEntryView;
import com.kingpixel.ultrasts.api.model.StsCategoryView;
import com.kingpixel.ultrasts.api.model.StsPokemonQuoteView;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.BuybackEntry;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing query and view model projection for UltraSTS.
 */
public class StsContextService {

  /**
   * Retrieves the full STS context for a player asynchronously.
   *
   * @param player the player
   * @return future containing PlayerStsContext
   */
  public CompletableFuture<PlayerStsContext> getPlayerContext(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        new PlayerStsContext(UUID.randomUUID(), "", Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), false, false)
      );
    }

    return CompletableFuture.supplyAsync(() -> {
      User user = UltraSTS.database.getUser(player);
      List<StsCategoryView> categories = new ArrayList<>();
      Map<String, String> earnings = new HashMap<>();

      boolean hasAnyAvailable = false;
      for (STS sts : STSConf.STS_MAP.values()) {
        StsCategoryView view = buildCategoryView(player, user, sts);
        categories.add(view);
        if (view.isAvailable()) {
          hasAnyAvailable = true;
        }
        if (user != null) {
          earnings.put(sts.getId(), user.getMoneyEarned(sts));
        }
      }

      boolean buybackEnabled = UltraSTS.config.getBuyback() != null && UltraSTS.config.getBuyback().isEnabled();
      List<StsBuybackEntryView> buybacks = new ArrayList<>();
      if (user != null && buybackEnabled && user.getBuyback() != null) {
        user.pruneExpiredBuyback();
        long maxAgeMillis = UltraSTS.config.getBuyback().getExpiration().toMillis();
        for (BuybackEntry entry : user.getBuyback()) {
          if (entry != null) {
            buybacks.add(buildBuybackView(entry, maxAgeMillis));
          }
        }
      }

      return new PlayerStsContext(
        player.getUuid(),
        player.getName().getString(),
        categories,
        buybacks,
        earnings,
        hasAnyAvailable,
        buybackEnabled
      );
    }, UltraSTS.getAsyncContext().getExecutor());
  }

  /**
   * Generates a price valuation quote for a Pokémon in an STS category.
   *
   * @param player  the player (for permissions context)
   * @param stsId   the STS category ID
   * @param pokemon the Pokémon to evaluate
   * @return future containing StsPokemonQuoteView
   */
  public CompletableFuture<StsPokemonQuoteView> quotePokemon(ServerPlayerEntity player, String stsId, Pokemon pokemon) {
    if (pokemon == null) {
      return CompletableFuture.completedFuture(
        new StsPokemonQuoteView(null, "", 0, false, stsId, BigDecimal.ZERO, "0", false, "Pokemon is null")
      );
    }

    STS sts = STSConf.getSTS(stsId);
    if (sts == null) {
      return CompletableFuture.completedFuture(
        new StsPokemonQuoteView(pokemon, PokemonUtils.replace(pokemon), pokemon.getLevel(), pokemon.getShiny(), stsId, BigDecimal.ZERO, "0", false, "STS category not found")
      );
    }

    BigDecimal price = BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
    String formattedPrice = EconomyApi.formatMoney(price, sts.getEconomy());

    String blockReason = "";
    boolean canSell = true;

    if (sts.isBlackListed(pokemon)) {
      canSell = false;
      blockReason = "Pokemon is blacklisted or holds a banned item";
    } else if (price.compareTo(BigDecimal.ZERO) <= 0) {
      canSell = false;
      blockReason = "Valuation is zero or negative";
    }

    return CompletableFuture.completedFuture(
      new StsPokemonQuoteView(
        pokemon,
        PokemonUtils.replace(pokemon),
        pokemon.getLevel(),
        pokemon.getShiny(),
        sts.getId(),
        price,
        formattedPrice,
        canSell,
        blockReason
      )
    );
  }

  private StsCategoryView buildCategoryView(ServerPlayerEntity player, User user, STS sts) {
    boolean hasPermission = user != null && user.hasPermission(player, sts);
    boolean onCooldown = user != null && user.hasCooldown(sts);
    long cdTimestamp = user != null ? user.getCooldown(sts) : 0L;
    long remainingMillis = Math.max(0L, cdTimestamp - System.currentTimeMillis());
    String formattedCooldown = PlayerUtils.getCooldown(cdTimestamp);

    String econType = sts.getEconomy() != null ? sts.getEconomy().getEconomyId() : "";
    String econCurrency = sts.getEconomy() != null ? sts.getEconomy().getCurrency() : "";

    List<String> lore = sts.getDisplay() != null && sts.getDisplay().getLore() != null
      ? new ArrayList<>(sts.getDisplay().getLore())
      : Collections.emptyList();

    return new StsCategoryView(
      sts.getId(),
      sts.getDisplay() != null ? sts.getDisplay().getDisplayname() : sts.getId(),
      sts.getDisplay(),
      hasPermission,
      onCooldown,
      remainingMillis,
      formattedCooldown,
      sts.isMultiSelect(),
      sts.getMaxSelected(),
      econType,
      econCurrency,
      lore
    );
  }

  private StsBuybackEntryView buildBuybackView(BuybackEntry entry, long maxAgeMillis) {
    STS sts = STSConf.getSTS(entry.getStsId());
    BigDecimal currentMarket = BigDecimal.ZERO;
    if (sts != null && entry.getPokemon() != null) {
      try {
        currentMarket = BigDecimal.valueOf(sts.getFormula().getPokemonValue(entry.getPokemon()));
      } catch (Exception ignored) {}
    }

    BigDecimal buybackPrice = (UltraSTS.config.getBuyback() != null)
      ? UltraSTS.config.getBuyback().calculateSecureBuybackPrice(entry.getOriginalPrice(), currentMarket)
      : entry.getOriginalPrice();

    String formattedBuyback = EconomyApi.formatMoney(buybackPrice, entry.getEconomy());
    long expiresAt = maxAgeMillis > 0 ? entry.getTimestamp() + maxAgeMillis : Long.MAX_VALUE;
    long remainingMillis = maxAgeMillis > 0 ? Math.max(0L, expiresAt - System.currentTimeMillis()) : Long.MAX_VALUE;
    String formattedRemaining = maxAgeMillis > 0 ? PlayerUtils.getCooldown(expiresAt) : "Permanent";
    boolean expired = entry.isExpired(maxAgeMillis);

    String pokeName = entry.getPokemon() != null ? PokemonUtils.replace(entry.getPokemon()) : "Unknown";
    int pokeLevel = entry.getPokemon() != null ? entry.getPokemon().getLevel() : 1;
    boolean shiny = entry.getPokemon() != null && entry.getPokemon().getShiny();

    return new StsBuybackEntryView(
      entry.getId(),
      entry.getPokemon(),
      pokeName,
      pokeLevel,
      shiny,
      entry.getStsId(),
      entry.getOriginalPrice(),
      buybackPrice,
      formattedBuyback,
      entry.getTimestamp(),
      expiresAt,
      remainingMillis,
      formattedRemaining,
      expired
    );
  }
}
