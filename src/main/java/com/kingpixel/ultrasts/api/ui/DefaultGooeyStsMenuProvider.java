package com.kingpixel.ultrasts.api.ui;

import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.gui.STSMenu;
import com.kingpixel.ultrasts.models.STS;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Default GooeyLibs-based menu provider for UltraSTS.
 */
public class DefaultGooeyStsMenuProvider implements StsMenuProvider {

  @Override
  public void openMainMenu(ServerPlayerEntity player) {
    if (player == null || UltraSTS.lang == null || UltraSTS.lang.getMenu() == null) return;
    UltraSTS.lang.getMenu().open(player);
  }

  @Override
  public void openCategoryMenu(ServerPlayerEntity player) {
    if (player == null || UltraSTS.lang == null || UltraSTS.lang.getStsCategoryMenu() == null) return;
    UltraSTS.lang.getStsCategoryMenu().open(player);
  }

  @Override
  public void openSellMenu(ServerPlayerEntity player, String stsId) {
    if (player == null) return;
    STS sts = STSConf.getSTS(stsId);
    if (sts != null) {
      STSMenu.open(player, sts);
    } else {
      openCategoryMenu(player);
    }
  }

  @Override
  public void openBuybackMenu(ServerPlayerEntity player, int page) {
    if (player == null || UltraSTS.lang == null || UltraSTS.lang.getBuybackMenu() == null) return;
    UltraSTS.lang.getBuybackMenu().open(player, Math.max(0, page));
  }

  @Override
  public void openProfileMenu(ServerPlayerEntity player) {
    if (player == null || UltraSTS.lang == null || UltraSTS.lang.getProfileMenu() == null) return;
    UltraSTS.lang.getProfileMenu().open(player);
  }

  @Override
  public void openLeaderboardMenu(ServerPlayerEntity player) {
    if (player == null || UltraSTS.lang == null) return;
    if (UltraSTS.lang.getSelectLeaderBoardMenu() != null) {
      UltraSTS.lang.getSelectLeaderBoardMenu().open(player);
    } else if (UltraSTS.lang.getLeaderBoardMenu() != null) {
      STS first = STSConf.STS_MAP.values().stream().findFirst().orElse(null);
      if (first != null) {
        UltraSTS.lang.getLeaderBoardMenu().open(player, first, 0);
      }
    }
  }
}
