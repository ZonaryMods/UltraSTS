package com.kingpixel.ultrasts.application;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.api.action.StsActionResult;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.BuybackEntry;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Application service providing headless actions for UltraSTS.
 */
public class StsActionService {

  /**
   * Sells a single Pokémon for the given player in the specified STS category.
   *
   * @param player  the player selling the Pokémon
   * @param stsId   the STS category ID
   * @param pokemon the Pokémon to sell
   * @return future containing action result
   */
  public CompletableFuture<StsActionResult> sellPokemon(ServerPlayerEntity player, String stsId, Pokemon pokemon) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "Player cannot be null")
      );
    }
    if (pokemon == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.POKEMON_NOT_FOUND, "Pokemon cannot be null", stsId)
      );
    }

    STS sts = STSConf.getSTS(stsId);
    if (sts == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.STS_NOT_FOUND, "STS category not found: " + stsId, stsId)
      );
    }

    User user = UltraSTS.database.getUser(player);
    if (user == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "User data not loaded for player", sts.getId())
      );
    }

    if (!user.hasPermission(player, sts)) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(
          StsActionResult.Status.PERMISSION_DENIED,
          "No permission for STS category: " + sts.getPermissionRequired(),
          sts.getId()
        )
      );
    }

    if (user.hasCooldown(sts)) {
      long cdRemaining = Math.max(0L, user.getCooldown(sts) - System.currentTimeMillis());
      return CompletableFuture.completedFuture(
        StsActionResult.cooldown("STS category is on cooldown", sts.getId(), cdRemaining)
      );
    }

    if (sts.isBlackListed(pokemon)) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.POKEMON_BLACKLISTED, "Pokemon is blacklisted or holding a banned item", sts.getId())
      );
    }

    BigDecimal price = BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
    if (price.compareTo(BigDecimal.ZERO) <= 0) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.POKEMON_BLACKLISTED, "Pokemon value is 0 or negative", sts.getId())
      );
    }

    return user.sellPokemon(sts, pokemon, player).thenApply(success -> {
      if (Boolean.TRUE.equals(success)) {
        return StsActionResult.successSell("Pokemon sold successfully", sts.getId(), price, 1);
      } else {
        return StsActionResult.failure(StsActionResult.Status.POKEMON_NOT_FOUND, "Pokemon could not be removed from party or PC", sts.getId());
      }
    });
  }

  /**
   * Sells a batch of Pokémon for the given player in the specified STS category.
   *
   * @param player   the player selling the Pokémon
   * @param stsId    the STS category ID
   * @param pokemons the list of Pokémon to sell
   * @return future containing action result
   */
  public CompletableFuture<StsActionResult> sellPokemons(ServerPlayerEntity player, String stsId, List<Pokemon> pokemons) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "Player cannot be null")
      );
    }
    if (pokemons == null || pokemons.isEmpty()) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.POKEMON_NOT_FOUND, "No Pokémon specified for sale", stsId)
      );
    }

    STS sts = STSConf.getSTS(stsId);
    if (sts == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.STS_NOT_FOUND, "STS category not found: " + stsId, stsId)
      );
    }

    User user = UltraSTS.database.getUser(player);
    if (user == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "User data not loaded for player", sts.getId())
      );
    }

    if (!user.hasPermission(player, sts)) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(
          StsActionResult.Status.PERMISSION_DENIED,
          "No permission for STS category: " + sts.getPermissionRequired(),
          sts.getId()
        )
      );
    }

    if (user.hasCooldown(sts)) {
      long cdRemaining = Math.max(0L, user.getCooldown(sts) - System.currentTimeMillis());
      return CompletableFuture.completedFuture(
        StsActionResult.cooldown("STS category is on cooldown", sts.getId(), cdRemaining)
      );
    }

    if (pokemons.size() > sts.getMaxSelected()) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(
          StsActionResult.Status.ERROR,
          "Exceeded maximum allowed Pokémon (" + sts.getMaxSelected() + ")",
          sts.getId()
        )
      );
    }

    List<Pokemon> validPokemons = new ArrayList<>();
    for (Pokemon p : pokemons) {
      if (p != null && !sts.isBlackListed(p) && sts.getFormula().getPokemonValue(p) > 0) {
        validPokemons.add(p);
      }
    }

    if (validPokemons.isEmpty()) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.POKEMON_BLACKLISTED, "No eligible Pokémon to sell in selection", sts.getId())
      );
    }

    return user.sellPokemons(sts, validPokemons, player).thenApply(totalPayout -> {
      if (totalPayout.compareTo(BigDecimal.ZERO) > 0) {
        return StsActionResult.successSell("Sold " + validPokemons.size() + " Pokémon", sts.getId(), totalPayout, validPokemons.size());
      } else {
        return StsActionResult.failure(StsActionResult.Status.ERROR, "Failed to sell selected Pokémon", sts.getId());
      }
    });
  }

  /**
   * Recovers a previously sold Pokémon from the player's buyback history.
   *
   * @param player  the player recovering the Pokémon
   * @param entryId the buyback entry UUID
   * @return future containing action result
   */
  public CompletableFuture<StsActionResult> buybackPokemon(ServerPlayerEntity player, UUID entryId) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "Player cannot be null")
      );
    }
    if (entryId == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.BUYBACK_NOT_FOUND, "Buyback entry ID cannot be null")
      );
    }
    if (UltraSTS.config.getBuyback() == null || !UltraSTS.config.getBuyback().isEnabled()) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.DISABLED, "Buyback system is disabled")
      );
    }

    User user = UltraSTS.database.getUser(player);
    if (user == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "User data not loaded for player")
      );
    }

    return user.buybackPokemon(entryId, player).thenApply(success -> {
      if (Boolean.TRUE.equals(success)) {
        return StsActionResult.successBuyback("Pokemon recovered successfully", BigDecimal.ZERO);
      } else {
        return StsActionResult.failure(StsActionResult.Status.ERROR, "Failed to recover Pokemon");
      }
    });
  }

  /**
   * Resets all STS cooldowns for the specified player.
   *
   * @param player the player whose cooldowns are reset
   * @return future containing action result
   */
  public CompletableFuture<StsActionResult> resetCooldowns(ServerPlayerEntity player) {
    if (player == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "Player cannot be null")
      );
    }

    User user = UltraSTS.database.getUser(player);
    if (user == null) {
      return CompletableFuture.completedFuture(
        StsActionResult.failure(StsActionResult.Status.ERROR, "User data not loaded for player")
      );
    }

    user.reset();
    user.save();
    return CompletableFuture.completedFuture(
      StsActionResult.success("Cooldowns reset successfully for " + player.getName().getString())
    );
  }
}
