package com.kingpixel.ultrasts.api.ui;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Thread-safe registry for custom UltraSTS menu providers.
 */
public final class StsMenuRegistry {

  private static final StsMenuProvider DEFAULT_PROVIDER = new DefaultGooeyStsMenuProvider();
  private static volatile StsMenuProvider currentProvider = DEFAULT_PROVIDER;

  private StsMenuRegistry() {}

  /**
   * Registers a custom menu provider.
   *
   * @param provider the custom provider, or null to revert to default
   */
  public static synchronized void register(StsMenuProvider provider) {
    currentProvider = (provider != null) ? provider : DEFAULT_PROVIDER;
  }

  /**
   * Unregisters the custom provider, reverting back to the default GooeyLibs implementation.
   */
  public static synchronized void unregister() {
    currentProvider = DEFAULT_PROVIDER;
  }

  /**
   * Checks whether a custom menu provider is currently active.
   *
   * @return true if a non-default provider is active
   */
  public static boolean hasCustomProvider() {
    return currentProvider != DEFAULT_PROVIDER;
  }

  /**
   * Retrieves the currently active menu provider.
   *
   * @return the active menu provider
   */
  public static StsMenuProvider getProvider() {
    return currentProvider;
  }

  /**
   * Opens the main STS menu using the active provider.
   *
   * @param player the player opening the menu
   */
  public static void openMainMenu(ServerPlayerEntity player) {
    currentProvider.openMainMenu(player);
  }

  /**
   * Opens the STS category selection menu using the active provider.
   *
   * @param player the player
   */
  public static void openCategoryMenu(ServerPlayerEntity player) {
    currentProvider.openCategoryMenu(player);
  }

  /**
   * Opens the STS sell menu for a specific STS category using the active provider.
   *
   * @param player the player
   * @param stsId  the STS category identifier
   */
  public static void openSellMenu(ServerPlayerEntity player, String stsId) {
    currentProvider.openSellMenu(player, stsId);
  }

  /**
   * Opens the buyback recovery menu using the active provider.
   *
   * @param player the player
   * @param page   the page number
   */
  public static void openBuybackMenu(ServerPlayerEntity player, int page) {
    currentProvider.openBuybackMenu(player, page);
  }

  /**
   * Opens the player profile menu using the active provider.
   *
   * @param player the player
   */
  public static void openProfileMenu(ServerPlayerEntity player) {
    currentProvider.openProfileMenu(player);
  }

  /**
   * Opens the global leaderboard menu using the active provider.
   *
   * @param player the player
   */
  public static void openLeaderboardMenu(ServerPlayerEntity player) {
    currentProvider.openLeaderboardMenu(player);
  }
}
