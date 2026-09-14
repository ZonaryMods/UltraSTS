package com.kingpixel.ultrasts.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import com.kingpixel.ultrasts.models.UserSTSSession;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.joml.Vector4f;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
public class STSSellMenu {
  private int rowsPC = 6;
  private int rowsParty = 4;
  private String title = "&6STS Sell Menu";
  private String titlePC = "&6STS Sell Menu (Box %box%)";
  private String titleParty = "&6STS Sell Menu (Party)";
  private List<Integer> partySlots = List.of(10, 11, 12, 13, 14, 15);

  private ItemModel clearButtonPC = ItemModel.builder()
    .slot(48)
    .item("minecraft:red_concrete")
    .displayname("&cClear Selection")
    .lore(List.of(
      "&7Click to clear your current selection"
    ))
    .build();

  private ItemModel clearButtonParty = ItemModel.builder()
    .slot(30)
    .item("minecraft:red_concrete")
    .displayname("&cClear Selection")
    .lore(List.of(
      "&7Click to clear your current selection"
    ))
    .build();

  private ItemModel confirmButtonPC = ItemModel.builder()
    .slot(53)
    .item("minecraft:green_concrete")
    .displayname("&aConfirm Sale (PC)")
    .lore(List.of(
      "&7Selected: &e%amount%",
      "&7Total Value: &e%price%",
      "",
      "&eClick to sell selected Pokémon!"
    ))
    .build();

  private ItemModel confirmButtonParty = ItemModel.builder()
    .slot(35)
    .item("minecraft:green_concrete")
    .displayname("&aConfirm Sale (Party)")
    .lore(List.of(
      "&7Selected: &e%amount%",
      "&7Total Value: &e%price%",
      "",
      "&eClick to sell selected Pokémon!"
    ))
    .build();

  private ItemModel pcButton = ItemModel.builder()
    .slot(27)
    .item("minecraft:chest")
    .displayname("&bShow PC")
    .build();

  private ItemModel partyButton = ItemModel.builder()
    .slot(45)
    .item("minecraft:ender_pearl")
    .displayname("&bShow Party")
    .build();

  private ItemModel nextBoxButton = ItemModel.builder()
    .slot(52)
    .item("minecraft:arrow")
    .displayname("&fNext Box")
    .build();

  private ItemModel prevBoxButton = ItemModel.builder()
    .slot(46)
    .item("minecraft:arrow")
    .displayname("&fPrevious Box")
    .build();

  private ItemModel closePCButton = ItemModel.builder()
    .slot(49)
    .item("minecraft:barrier")
    .displayname("&cClose PC")
    .build();

  private ItemModel closePartyButton = ItemModel.builder()
    .slot(31)
    .item("minecraft:barrier")
    .displayname("&cClose Party")
    .build();

  private List<PanelsConfig> panels = List.of(new PanelsConfig());

  // State
  private transient Cache<String, UserSTSSession> sessions = Caffeine.newBuilder()
    .expireAfterAccess(15, TimeUnit.MINUTES)
    .maximumSize(500)
    .build();

  private String getSessionKey(UUID uuid, String stsId) {
    return uuid.toString() + ":" + stsId;
  }

  public void open(ServerPlayerEntity player, STS sts, boolean clear) {
    String key = getSessionKey(player.getUuid(), sts.getId());
    if (sessions.getIfPresent(key) == null) {
      sessions.put(key, new UserSTSSession(player.getUuid(), sts.getId()));
    }

    if (clear) Objects.requireNonNull(sessions.getIfPresent(key)).clearSelection();

    render(player, sts);
  }

  private void render(ServerPlayerEntity player, STS sts) {
    UserSTSSession session = sessions.getIfPresent(getSessionKey(player.getUuid(), sts.getId()));
    if (session == null) {
      open(player, sts, true);
      return;
    }
    boolean isPC = session.isShowingPC();
    int rows = isPC ? rowsPC : rowsParty;

    ChestTemplate template = ChestTemplate.builder(rows).build();
    PanelsConfig.applyConfig(template, panels);

    Set<Pokemon> selected = session.getSelectedPokemon();
    int boxIdx = session.getCurrentBox();

    // Filter and Display Pokémon
    if (isPC) {
      displayPC(player, sts, template, selected, boxIdx);
      partyButton.applyTemplate(template, partyButton.getButton(action -> {
        session.setShowingPC(false);
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      clearButtonPC.applyTemplate(template, clearButtonPC.getButton(action -> {
        session.clearSelection();
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      prevBoxButton.applyTemplate(template, prevBoxButton.getButton(action -> {
        int max = Cobblemon.INSTANCE.getStorage().getPC(player).getBoxes().size();
        session.setCurrentBox((boxIdx - 1 + max) % max);
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      nextBoxButton.applyTemplate(template, nextBoxButton.getButton(action -> {
        int max = Cobblemon.INSTANCE.getStorage().getPC(player).getBoxes().size();
        session.setCurrentBox((boxIdx + 1) % max);
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      closePCButton.applyTemplate(template, closePCButton.getButton(action -> UltraSTS.lang.getMenu().open(player), 1, TimeUnit.SECONDS, 1));
    } else {
      displayParty(player, sts, template, selected);
      pcButton.applyTemplate(template, pcButton.getButton(action -> {
        session.setShowingPC(true);
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      clearButtonParty.applyTemplate(template, clearButtonParty.getButton(action -> {
        session.clearSelection();
        render(player, sts);
      }, 1, TimeUnit.SECONDS, 1));

      closePartyButton.applyTemplate(template, closePartyButton.getButton(action -> UltraSTS.lang.getMenu().open(player), 1, TimeUnit.SECONDS, 1));
    }

    // Confirm Button
    BigDecimal totalPrice = BigDecimal.ZERO;
    for (Pokemon p : selected) {
      totalPrice = totalPrice.add(BigDecimal.valueOf(sts.getFormula().getPokemonValue(p)));
    }

    BigDecimal finalTotalPrice = totalPrice;
    ItemModel confirmModel = isPC ? confirmButtonPC : confirmButtonParty;

    List<String> lore = confirmModel.getLore() != null
      ? confirmModel.getLore().stream()
          .map(s -> s.replace("%amount%", String.valueOf(selected.size()))
            .replace("%price%", EconomyApi.formatMoney(finalTotalPrice, sts.getEconomy())))
          .collect(Collectors.toList())
      : new ArrayList<>();

    confirmModel.applyTemplate(template, confirmModel.getButton(1, null, lore, action -> {
      if (selected.isEmpty()) return;
      if (sts.isMultiSelect()) {
        for (Pokemon p : selected) {
          if (UltraSTS.config.isItemBanned(p)) {
            PlayerUtils.sendMessage(player, UltraSTS.lang.getItemBannedMessage(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
            return;
          }
        }
      }
      handleSale(player, sts, selected);
    }, 1, TimeUnit.SECONDS, 1));

    String pageTitle = isPC
      ? titlePC.replace("%box%", String.valueOf(boxIdx + 1))
      : titleParty;

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(pageTitle))
      .build();

    CobbleUtils.server.execute(() -> UIManager.openUIForcefully(player, page));
  }

  private void displayParty(ServerPlayerEntity player, STS sts, ChestTemplate template, Set<Pokemon> selected) {
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);

    for (int i = 0; i < 6; i++) {

      if (i >= party.size()) {
        template.set(
          partySlots.get(i),
          GooeyButton.of(ItemStack.EMPTY)
        );
        continue;
      }

      Pokemon pokemon = party.get(i);

      if (sts.isBlackListed(pokemon)) {
        template.set(
          partySlots.get(i),
          GooeyButton.of(CobblemonItems.POKE_BALL.getDefaultStack())
        );
        continue;
      }

      template.set(
        partySlots.get(i),
        createPokemonButton(player, sts, pokemon, selected)
      );
    }
  }

  private void displayPC(ServerPlayerEntity player, STS sts, ChestTemplate template, Set<Pokemon> selected, int boxIdx) {
    var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
    PCBox box = pc.getBoxes().get(boxIdx);
    int slot = 0;
    for (int i = 0; i < 30; i++) {
      Pokemon pokemon = box.get(i);
      if (pokemon == null || sts.isBlackListed(pokemon)) {
        template.set(slot++, GooeyButton.of(CobblemonItems.POKE_BALL.getDefaultStack()));
      } else {
        template.set(slot++, createPokemonButton(player, sts, pokemon, selected));
      }
    }
  }

  private GooeyButton createPokemonButton(ServerPlayerEntity player, STS sts, Pokemon pokemon, Set<Pokemon> selected) {
    boolean isSelected = selected.contains(pokemon);
    ItemStack itemStack = isSelected
      ? PokemonItem.from(pokemon, 1, new Vector4f(1f, 1f, 1f, 1.0f))
      : PokemonItem.from(pokemon, 1, new Vector4f(0.5f, 0.5f, 0.5f, 0.5f));

    if (isSelected) itemStack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

    List<String> lore = UltraSTS.lang.getPokemonLore() != null
      ? new ArrayList<>(UltraSTS.lang.getPokemonLore())
      : new ArrayList<>();
    EconomyUse economyUse = sts.getEconomy();
    if (isSelected && UltraSTS.lang.getSelected() != null) {
      lore.addFirst(UltraSTS.lang.getSelected());
    }
    String price = EconomyApi.formatMoney(BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon)), economyUse);
    lore.replaceAll(s -> PokemonUtils.replace(
      s.replace("%price%", price),
      pokemon
    ));

    return GooeyButton.builder()
      .display(itemStack)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.replace(pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
      .onClick(action -> {
        if (selected.contains(pokemon)) {
          selected.remove(pokemon);
        } else {
          if (sts.isMultiSelect()) {
            if (selected.size() >= sts.getMaxSelected()) {
              PlayerUtils.sendMessage(
                player,
                UltraSTS.lang.getMaxSelectedMessage()
                  .replace("%amount%", String.valueOf(sts.getMaxSelected())),
                UltraSTS.lang.getPrefix(),
                TypeMessage.CHAT
              );
              return;
            }
            selected.add(pokemon);
          } else {
            selected.clear();
            selected.add(pokemon);
          }
        }
        render(player, sts);
      })
      .build();
  }

  private void handleSale(ServerPlayerEntity player, STS sts, Set<Pokemon> selected) {
    User user = UltraSTS.database.getUser(player.getUuid());
    if (user == null) return;

    List<Pokemon> toSell = new ArrayList<>(selected);

    if (toSell.isEmpty()) {
      PlayerUtils.sendMessage(player, UltraSTS.lang.getNoPokemonSelected(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
      return;
    }

    user.sellPokemons(sts, toSell, player)
      .whenComplete((totalPrice, throwable) -> {
        if (throwable != null) {
          throwable.printStackTrace();
          PlayerUtils.sendMessage(player, UltraSTS.lang.getErrorProcessingSale(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
          return;
        }

        if (totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
          PlayerUtils.sendMessage(player, UltraSTS.lang.getNoValidPokemonSold(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
          return;
        }

        CobbleUtils.server.execute(() -> {

          PlayerUtils.sendMessage(player,
            UltraSTS.lang.getNotificationSellingMulti()
              .replace("%amount%", String.valueOf(toSell.size()))
              .replace("%price%", EconomyApi.formatMoney(totalPrice, sts.getEconomy())),
            UltraSTS.lang.getPrefix(),
            TypeMessage.CHAT);

          UserSTSSession session = sessions.getIfPresent(getSessionKey(player.getUuid(), sts.getId()));
          if (session != null) session.clearSelection();
          UltraSTS.lang.getMenu().open(player);
        });
      });
  }
}
