package com.kingpixel.ultrasts.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.models.BuybackEntry;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.joml.Vector4f;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Data
public class BuybackMenu {
  private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n|\\\\n");

  private int rows = 6;
  private String title = "&6Recover Sold Pokémon (%page%/%max_pages%)";
  private Rectangle rectangle = new Rectangle(rows);

  private List<String> pokemonLore = List.of(
    "%lorepokemon%",
    "&7Original Sell Price: &e%original_price%",
    "&7Buyback Price: &a%price%",
    "&7Expires in: &c%expires_in%",
    "",
    "&eClick to recover this Pokémon!"
  );

  private ItemModel previousPage = ItemModel.builder()
    .slot(rows * 9 - 7)
    .item("minecraft:arrow")
    .displayname("&6Previous Page")
    .lore(List.of("&7Go to previous page"))
    .build();

  private ItemModel close = ItemModel.builder()
    .slot(rows * 9 - 5)
    .item("minecraft:barrier")
    .displayname("&6Close")
    .lore(List.of("&7Return to main menu"))
    .build();

  private ItemModel nextPage = ItemModel.builder()
    .slot(rows * 9 - 3)
    .item("minecraft:arrow")
    .displayname("&6Next Page")
    .lore(List.of("&7Go to next page"))
    .build();

  private List<PanelsConfig> panels = List.of(new PanelsConfig(rows));

  public CompletableFuture<Void> open(ServerPlayerEntity player, int page) {
    User user = UltraSTS.database.getUser(player);
    if (user == null) {
      UltraSTS.lang.getMenu().open(player);
      return CompletableFuture.completedFuture(null);
    }

    user.pruneExpiredBuyback();
    List<BuybackEntry> allEntries = user.getBuyback();

    return UltraSTS.getAsyncContext().runAsync(() -> {
      ChestTemplate template = ChestTemplate.builder(rows).build();
      PanelsConfig.applyConfig(template, panels);

      int slotsPerPage = Math.max(1, rectangle.getTotalSlots());
      int totalItems = allEntries != null ? allEntries.size() : 0;
      int maxPages = Math.max(1, (int) Math.ceil((double) totalItems / slotsPerPage));
      int safePage = Math.min(Math.max(0, page), maxPages - 1);

      List<GooeyButton> buttons = populatePageButtons(player, user, allEntries, safePage, slotsPerPage);
      rectangle.apply(template, buttons);

      if (safePage > 0) {
        previousPage.applyTemplate(template, previousPage.getButton(action -> open(player, safePage - 1), 1, TimeUnit.SECONDS, 1));
      }

      close.applyTemplate(template, close.getButton(action -> UltraSTS.lang.getMenu().open(player), 1, TimeUnit.SECONDS, 1));

      if (safePage < maxPages - 1) {
        nextPage.applyTemplate(template, nextPage.getButton(action -> open(player, safePage + 1), 1, TimeUnit.SECONDS, 1));
      }

      String resolvedTitle = title
        .replace("%page%", String.valueOf(safePage + 1))
        .replace("%max_pages%", String.valueOf(maxPages));

      GooeyPage gooeyPage = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(resolvedTitle))
        .build();

      CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, gooeyPage));
    });
  }

  private List<GooeyButton> populatePageButtons(ServerPlayerEntity player, User user, List<BuybackEntry> allEntries, int page, int slotsPerPage) {
    List<GooeyButton> buttons = new ArrayList<>();
    if (allEntries == null || allEntries.isEmpty()) return buttons;

    List<BuybackEntry> reversed = new ArrayList<>(allEntries);
    Collections.reverse(reversed);

    int startIndex = page * slotsPerPage;
    int endIndex = Math.min(startIndex + slotsPerPage, reversed.size());

    for (int i = startIndex; i < endIndex; i++) {
      BuybackEntry entry = reversed.get(i);
      if (entry != null && entry.getPokemon() != null) {
        buttons.add(createPokemonButton(player, user, entry, page));
      }
    }
    return buttons;
  }

  private GooeyButton createPokemonButton(ServerPlayerEntity player, User user, BuybackEntry entry, int currentPage) {
    Pokemon pokemon = entry.getPokemon();
    ItemStack itemStack = PokemonItem.from(pokemon, 1, new Vector4f(1f, 1f, 1f, 1f));

    List<String> lore = buildLore(entry);

    return GooeyButton.builder()
      .display(itemStack)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
      .onClick(action -> {
        ItemStack confirmDisplay = itemStack.copy();
        UltraSTS.lang.getConfirmMenu().open(
          player,
          confirmDisplay,
          confirmAction -> user.buybackPokemon(entry.getId(), player)
            .whenComplete((success, err) -> CobbleUtils.server.execute(() -> open(player, currentPage))),
          cancelAction -> CobbleUtils.server.execute(() -> open(player, currentPage))
        );
      })
      .build();
  }

  private List<String> buildLore(BuybackEntry entry) {
    Pokemon pokemon = entry.getPokemon();
    List<String> lore = new ArrayList<>();

    long maxAgeMillis = UltraSTS.config.getBuyback().getExpiration().toMillis();
    String remainingTime = entry.getRemainingTime(maxAgeMillis);

    STS sts = STSConf.getSTS(entry.getStsId());
    if (sts == null) {
      sts = STSConf.getSTS("");
    }

    EconomyUse economy = entry.getEconomy();
    if (economy == null && sts != null) {
      economy = sts.getEconomy();
    }

    BigDecimal originalPrice = entry.getOriginalPrice() != null ? entry.getOriginalPrice() : BigDecimal.ZERO;
    BigDecimal currentMarketPrice = BigDecimal.ZERO;
    if (sts != null && pokemon != null) {
      try {
        currentMarketPrice = BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
      } catch (Exception ignored) {
        // Fallback
      }
    }

    BigDecimal buybackPrice = UltraSTS.config.getBuyback().calculateSecureBuybackPrice(originalPrice, currentMarketPrice);

    String formattedOriginalPrice = EconomyApi.formatMoney(originalPrice, economy);
    String formattedBuybackPrice = EconomyApi.formatMoney(buybackPrice, economy);

    List<String> rawLore = pokemonLore != null ? pokemonLore : List.of();
    for (String line : rawLore) {
      appendProcessedLoreLine(lore, line, pokemon, formattedOriginalPrice, formattedBuybackPrice, remainingTime);
    }

    return lore;
  }

  private void appendProcessedLoreLine(List<String> lore, String line, Pokemon pokemon, String originalPrice, String buybackPrice, String remainingTime) {
    if (line == null) return;
    String processed = line
      .replace("%original_price%", originalPrice)
      .replace("%price%", buybackPrice)
      .replace("%expires_in%", remainingTime);

    if (processed.contains("%lorepokemon%")) {
      List<String> pokeLore = PokemonUtils.replaceLore(pokemon);
      if (pokeLore != null && !pokeLore.isEmpty()) {
        for (String pLine : pokeLore) {
          if (pLine != null) {
            Collections.addAll(lore, NEWLINE_PATTERN.split(pLine));
          }
        }
      }
    } else {
      String replaced = PokemonUtils.replace(processed, pokemon);
      Collections.addAll(lore, NEWLINE_PATTERN.split(replaced));
    }
  }
}
