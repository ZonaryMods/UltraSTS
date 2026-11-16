package com.kingpixel.ultrasts.api;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.api.action.StsActionResult;
import com.kingpixel.ultrasts.api.model.PlayerStsContext;
import com.kingpixel.ultrasts.api.model.StsPokemonQuoteView;
import com.kingpixel.ultrasts.api.ui.StsMenuProvider;
import com.kingpixel.ultrasts.api.ui.StsMenuRegistry;
import com.kingpixel.ultrasts.application.StsActionService;
import com.kingpixel.ultrasts.application.StsContextService;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Unified Public API for UltraSTS.
 * Primary entry point for companion mods (Rotom Phone, custom GUIs, Web panels)
 * to interact with the STS system headlessly or via pluggable UI providers.
 */
public final class UltraStsApi {

  private static final StsActionService ACTION_SERVICE = new StsActionService();
  private static final StsContextService CONTEXT_SERVICE = new StsContextService();

  private UltraStsApi() {}

  // ================= UI / Menu Provider Methods =================

  /**
   * Opens the main STS menu using the active menu provider.
   *
   * @param player the player opening the menu
   */
  public static void openMainMenu(ServerPlayerEntity player) {
    StsMenuRegistry.openMainMenu(player);
  }

  /**
   * Opens the STS category selection menu using the active menu provider.
   *
   * @param player the player
   */
  public static void openCategoryMenu(ServerPlayerEntity player) {
    StsMenuRegistry.openCategoryMenu(player);
  }

  /**
   * Opens the STS sell menu for a category using the active menu provider.
   *
   * @param player the player
   * @param stsId  the STS category ID
   */
  public static void openSellMenu(ServerPlayerEntity player, String stsId) {
    StsMenuRegistry.openSellMenu(player, stsId);
  }

  /**
   * Opens the buyback recovery menu using the active menu provider.
   *
   * @param player the player
   * @param page   the page number
   */
  public static void openBuybackMenu(ServerPlayerEntity player, int page) {
    StsMenuRegistry.openBuybackMenu(player, page);
  }

  /**
   * Opens the profile menu using the active menu provider.
   *
   * @param player the player
   */
  public static void openProfileMenu(ServerPlayerEntity player) {
    StsMenuRegistry.openProfileMenu(player);
  }

  /**
   * Opens the global leaderboard menu using the active menu provider.
   *
   * @param player the player
   */
  public static void openLeaderboardMenu(ServerPlayerEntity player) {
    StsMenuRegistry.openLeaderboardMenu(player);
  }

  /**
   * Registers a custom menu provider.
   *
   * @param provider the custom provider to register
   */
  public static void registerMenuProvider(StsMenuProvider provider) {
    StsMenuRegistry.register(provider);
  }

  /**
   * Unregisters the custom menu provider, reverting back to the default GooeyLibs implementation.
   */
  public static void unregisterMenuProvider() {
    StsMenuRegistry.unregister();
  }

  /**
   * Checks if a custom menu provider is currently active.
   *
   * @return true if a custom provider is registered
   */
  public static boolean hasCustomMenuProvider() {
    return StsMenuRegistry.hasCustomProvider();
  }

  /**
   * Retrieves the active menu provider.
   *
   * @return the active provider
   */
  public static StsMenuProvider getMenuProvider() {
    return StsMenuRegistry.getProvider();
  }

  // ================= Headless Action Methods =================

  /**
   * Sells a single Pokémon for the given player headlessly.
   *
   * @param player  the player
   * @param stsId   the STS category ID
   * @param pokemon the Pokémon to sell
   * @return future containing action result
   */
  public static CompletableFuture<StsActionResult> sellPokemon(ServerPlayerEntity player, String stsId, Pokemon pokemon) {
    return ACTION_SERVICE.sellPokemon(player, stsId, pokemon);
  }

  /**
   * Sells a batch of Pokémon for the given player headlessly.
   *
   * @param player   the player
   * @param stsId    the STS category ID
   * @param pokemons the list of Pokémon to sell
   * @return future containing action result
   */
  public static CompletableFuture<StsActionResult> sellPokemons(ServerPlayerEntity player, String stsId, List<Pokemon> pokemons) {
    return ACTION_SERVICE.sellPokemons(player, stsId, pokemons);
  }

  /**
   * Recovers a previously sold Pokémon from buyback history headlessly.
   *
   * @param player  the player
   * @param entryId the buyback entry UUID
   * @return future containing action result
   */
  public static CompletableFuture<StsActionResult> buybackPokemon(ServerPlayerEntity player, UUID entryId) {
    return ACTION_SERVICE.buybackPokemon(player, entryId);
  }

  /**
   * Resets all STS cooldowns for the given player headlessly.
   *
   * @param player the target player
   * @return future containing action result
   */
  public static CompletableFuture<StsActionResult> resetCooldowns(ServerPlayerEntity player) {
    return ACTION_SERVICE.resetCooldowns(player);
  }

  // ================= Query / View Context Methods =================

  /**
   * Retrieves the complete STS context for a player.
   *
   * @param player the player
   * @return future containing PlayerStsContext
   */
  public static CompletableFuture<PlayerStsContext> getPlayerContext(ServerPlayerEntity player) {
    return CONTEXT_SERVICE.getPlayerContext(player);
  }

  /**
   * Generates a valuation quote for a Pokémon in an STS category.
   *
   * @param player  the player
   * @param stsId   the STS category ID
   * @param pokemon the Pokémon
   * @return future containing StsPokemonQuoteView
   */
  public static CompletableFuture<StsPokemonQuoteView> quotePokemon(ServerPlayerEntity player, String stsId, Pokemon pokemon) {
    return CONTEXT_SERVICE.quotePokemon(player, stsId, pokemon);
  }

  // ================= Service & Data Access =================

  public static StsActionService getActionService() {
    return ACTION_SERVICE;
  }

  public static StsContextService getContextService() {
    return CONTEXT_SERVICE;
  }

  public static STS getSTS(String stsId) {
    return STSConf.getSTS(stsId);
  }

  public static Collection<STS> getCategories() {
    return STSConf.STS_MAP.values();
  }

  public static User getUser(ServerPlayerEntity player) {
    return UltraSTS.database != null ? UltraSTS.database.getUser(player) : null;
  }

  public static User getUser(UUID uuid) {
    return UltraSTS.database != null ? UltraSTS.database.getUser(uuid) : null;
  }
}
