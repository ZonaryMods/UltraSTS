package com.kingpixel.ultrasts.api.ui;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Service Provider Interface (SPI) for UltraSTS menu rendering.
 * Allows external companion mods (Rotom Phone, Custom GUI plugins, Web integrations)
 * to intercept menu opening and provide custom interfaces.
 */
public interface StsMenuProvider {

  /**
   * Opens the main STS menu (hub with categories, profile, leaderboard, buyback).
   *
   * @param player the player opening the menu
   */
  void openMainMenu(ServerPlayerEntity player);

  /**
   * Opens the STS category selection menu.
   *
   * @param player the player opening the categories
   */
  void openCategoryMenu(ServerPlayerEntity player);

  /**
   * Opens the STS sell menu for a specific STS category.
   *
   * @param player the player
   * @param stsId  the STS category identifier
   */
  void openSellMenu(ServerPlayerEntity player, String stsId);

  /**
   * Opens the buyback recovery menu for previously sold Pokémon.
   *
   * @param player the player
   * @param page   the page index (0-based)
   */
  void openBuybackMenu(ServerPlayerEntity player, int page);

  /**
   * Opens the player profile and stats menu.
   *
   * @param player the player
   */
  void openProfileMenu(ServerPlayerEntity player);

  /**
   * Opens the global STS leaderboard menu.
   *
   * @param player the player
   */
  void openLeaderboardMenu(ServerPlayerEntity player);
}
