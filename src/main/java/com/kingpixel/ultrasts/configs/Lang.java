package com.kingpixel.ultrasts.configs;

import com.kingpixel.cobbleutils.ui.ConfirmMenu;
import com.kingpixel.cobbleutils.ui.PartyPcMenu;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.gui.*;
import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Data
public class Lang {
  private String prefix = "&7[&6UltraSTS&7] &r";
  private String selected = "&7[&aSelected&7]";
  private String cooldownMessage = "%prefix% &cYou are on cooldown for %time%!";
  private String notificationSTSAvailable = "&aYou have some STS available!";
  private String notificationSelling = "%prefix% &6You sold %pokemon% for %price%!";
  private String notificationSellingMulti = "%prefix% &6You sold %amount% Pokémon for %price%!";
  private String maxSelectedMessage = "%prefix% &cYou can only select up to %amount% Pokemon in this STS!";
  private String itemBannedMessage = "%prefix% &cOne or more selected Pokemon have a banned item!";
  private List<String> pokemonLore = List.of(
    "%lorepokemon%",
    "&7Sell this pokemon for STS!",
    "&7Value: %price% STS"
  );
  private String noPermissionCategory = "%prefix% &cYou do not have permission to enter this STS category! (Permission: %permission%)";
  private String cannotJoinCategory = "%prefix% &cYou cannot join this STS category!";
  private String errorSellingPokemon = "%prefix% &cAn error occurred while selling this pokemon!";
  private String cannotSellPokemon = "%prefix% &cYou cannot sell this pokemon!";
  private String noPokemonSelected = "%prefix% &cNo Pokemon selected!";
  private String errorProcessingSale = "%prefix% &cError processing sale.";
  private String noValidPokemonSold = "%prefix% &cNo valid Pokemon were sold (perhaps they no longer exist?).";
  private String errorLoadingProfile = "&cAn error occurred while loading your profile. Please try again later.";
  private String reloadSuccess = "%prefix% &aUltraSTS reloaded successfully!";
  private String resetSuccess = "%prefix% &aCooldowns have been reset successfully!";
  private String resetSuccessOther = "%prefix% &aCooldowns for %player% have been reset successfully!";
  private String playerNotFound = "%prefix% &cPlayer %player% not found!";
  private String unknownUser = "Unknown";
  private String buybackDisabled = "%prefix% &cPokemon recovery is currently disabled.";
  private String buybackExpired = "%prefix% &cThis Pokemon has expired and can no longer be recovered.";
  private String buybackSuccess = "%prefix% &aSuccessfully recovered %pokemon% for %price%!";
  private String buybackNotEnoughMoney = "%prefix% &cYou do not have enough money to recover this Pokemon! (Required: %price%)";
  private String buybackStorageFull = "%prefix% &cYour party and PC are full! Please make space before recovering.";
  private String buybackNotFound = "%prefix% &cPokemon not found in recovery history.";
  private String noPermissionBuyback = "%prefix% &cYou do not have permission to recover sold Pokemon! (Permission: %permission%)";
  private String buybackPermanent = "Permanent";
  // GUI
  private Menu menu = new Menu();
  private STSCategoryMenu stsCategoryMenu = new STSCategoryMenu();
  private ProfileMenu profileMenu = new ProfileMenu();
  private PartyPcMenu partyPcMenu = new PartyPcMenu();
  private ConfirmMenu confirmMenu = new ConfirmMenu();
  private SelectLeaderBoardMenu selectLeaderBoardMenu = new SelectLeaderBoardMenu();
  private LeaderBoardMenu leaderBoardMenu = new LeaderBoardMenu();
  private STSSellMenu stsSellMenu = new STSSellMenu();
  private BuybackMenu buybackMenu = new BuybackMenu();

  public static void init() {
    String language = UltraSTS.config.getLang();
    Path file = UltraSTS.getPath().resolve("lang").resolve(language + ".json");
    try {
      Lang lang = UtilsFile.read(file, Lang.class);
      if (lang == null)
        lang = new Lang();
      lang.fix();
      UltraSTS.lang = lang;
      UtilsFile.writeAsync(file, lang);
    } catch (IOException e) {
      UltraSTS.LOGGER.error("Failed to load or save language file", e);
    }
  }

  private void fix() {
    if (resetSuccess == null) resetSuccess = "%prefix% &aCooldowns have been reset successfully!";
    if (resetSuccessOther == null) resetSuccessOther = "%prefix% &aCooldowns for %player% have been reset successfully!";
    if (playerNotFound == null) playerNotFound = "%prefix% &cPlayer %player% not found!";
    if (buybackMenu == null) buybackMenu = new BuybackMenu();
    if (buybackDisabled == null) buybackDisabled = "%prefix% &cPokemon recovery is currently disabled.";
    if (buybackExpired == null) buybackExpired = "%prefix% &cThis Pokemon has expired and can no longer be recovered.";
    if (buybackSuccess == null) buybackSuccess = "%prefix% &aSuccessfully recovered %pokemon% for %price%!";
    if (buybackNotEnoughMoney == null) buybackNotEnoughMoney = "%prefix% &cYou do not have enough money to recover this Pokemon! (Required: %price%)";
    if (buybackStorageFull == null) buybackStorageFull = "%prefix% &cYour party and PC are full! Please make space before recovering.";
    if (buybackNotFound == null) buybackNotFound = "%prefix% &cPokemon not found in recovery history.";
    if (noPermissionBuyback == null) noPermissionBuyback = "%prefix% &cYou do not have permission to recover sold Pokemon! (Permission: %permission%)";
    if (buybackPermanent == null) buybackPermanent = "Permanent";
  }
}
