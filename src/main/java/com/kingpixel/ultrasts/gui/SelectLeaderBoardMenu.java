package com.kingpixel.ultrasts.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.STS;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Data
public class SelectLeaderBoardMenu {
  private int rows = 3;
  private String title = "&6Select Leaderboard";
  private ItemModel close = ItemModel.builder()
    .slot(22)
    .item("minecraft:barrier")
    .displayname("&6Close")
    .lore(List.of("&7Close the leaderboard menu"))
    .build();
  private List<PanelsConfig> panels = List.of(new PanelsConfig());

  public CompletableFuture<Void> open(ServerPlayerEntity player) {
    return UltraSTS.getAsyncContext().runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(rows)
        .build();

      PanelsConfig.applyConfig(template, panels);

      for (STS value : STSConf.STS_MAP.values()) {
        ItemModel stsDisplay = value.getDisplay();
        if (stsDisplay == null) continue;
        ItemModel display = ItemModel.builder()
          .item(stsDisplay.getItem())
          .displayname(stsDisplay.getDisplayname())
          .lore(stsDisplay.getLore() != null ? new java.util.ArrayList<>(stsDisplay.getLore()) : new java.util.ArrayList<>())
          .slot(stsDisplay.getSlot())
          .build();
        display.applyTemplate(template, display.getButton(action -> UltraSTS.lang.getLeaderBoardMenu().open(player, value, 0), 1, TimeUnit.SECONDS, 1));
      }

      close.applyTemplate(template, close.getButton(action -> UltraSTS.lang.getMenu().open(player), 1, TimeUnit.SECONDS, 1));

      GooeyPage page = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(title))
        .build();

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }
}
