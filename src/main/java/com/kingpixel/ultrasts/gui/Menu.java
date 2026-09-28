package com.kingpixel.ultrasts.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.STS;
import lombok.Data;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Data
public class Menu {
  private int rows = 3;
  private String title = "&6STS";
  private ItemModel leaderboard = ItemModel.builder()
    .slot(10)
    .item("minecraft:beacon")
    .displayname("&6Leaderboards")
    .lore(List.of("&7View the leaderboard of the top STS earners!"))
    .build();
  private ItemModel sts = ItemModel.builder()
    .slot(13)
    .item("minecraft:emerald")
    .displayname("&6STS Categories")
    .lore(List.of("&7Sell pokemon for STS!"))
    .build();
  private ItemModel profile = ItemModel.builder()
    .slot(16)
    .item("minecraft:player_head")
    .displayname("&6Profile")
    .lore(List.of("&7View your profile and stats!"))
    .build();
  private List<PanelsConfig> panels = List.of(new PanelsConfig());

  public CompletableFuture<Void> open(ServerPlayerEntity player) {
    return UltraSTS.getAsyncContext().runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(rows)
        .build();

      PanelsConfig.applyConfig(template, panels);

      leaderboard.applyTemplate(template, leaderboard.getButton(action -> {
        if (STSConf.getSize() == 1) {
          STS selectSTS = STSConf.getSTS("");
          UltraSTS.lang.getLeaderBoardMenu().open(player, selectSTS, 0);
        } else {
          UltraSTS.lang.getSelectLeaderBoardMenu().open(player);
        }
      }, 1, TimeUnit.SECONDS, 1));


      if (STSConf.getSize() == 1) {
        STS selectSTS = STSConf.getSTS("");
        sts.applyTemplate(template, selectSTS.getButton(player));
      } else {
        sts.applyTemplate(template, sts.getButton(action -> UltraSTS.lang.getStsCategoryMenu().open(player), 1, TimeUnit.SECONDS, 1));
      }

      profile.applyTemplate(template, getProfileButton(player));

      GooeyPage page = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(title))
        .build();
      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }

  private RateLimitedButton getProfileButton(ServerPlayerEntity player) {
    RateLimitedButton profileButton = profile.getButton(action -> UltraSTS.lang.getProfileMenu().open(player), 1, TimeUnit.SECONDS, 1);
    if (profile.getItem() != null && profile.getItem().contains("player_head")) {
      ItemStack headItem = PlayerUtils.getHeadItem(player);
      headItem.applyComponentsFrom(profileButton.getDisplay().getComponents());
      profileButton.setDisplay(headItem);
    }
    return profileButton;
  }
}
