package com.kingpixel.ultrasts.gui;

import ca.landonjw.gooeylibs2.api.UIManager;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Data
public class STSMenu {

  public static CompletableFuture<Void> open(ServerPlayerEntity player, STS selectSTS) {

    User user = UltraSTS.database.getUser(player);

    if (user == null) {
      PlayerUtils.sendMessage(player, UltraSTS.lang.getCannotJoinCategory(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
      UltraSTS.lang.getMenu().open(player);
      return CompletableFuture.completedFuture(null);
    }

    if (user.hasCooldown(selectSTS)) {
      PlayerUtils.sendMessage(
        player,
        UltraSTS.lang.getCooldownMessage()
          .replace("%time%", PlayerUtils.getCooldown(user.getCooldown(selectSTS))),
        UltraSTS.lang.getPrefix(),
        TypeMessage.CHAT
      );
      return CompletableFuture.completedFuture(null);
    }

    if (selectSTS.isMultiSelect()) {
      UltraSTS.lang.getStsSellMenu().open(player, selectSTS, true);
      return CompletableFuture.completedFuture(null);
    }

    return UltraSTS.getAsyncContext().runAsync(() -> UltraSTS.lang.getPartyPcMenu().openParty(
      player,
      template -> {
      },
      pokemonButtonAction -> user.sellPokemon(selectSTS, pokemonButtonAction.getPokemon(), player)
        .whenComplete((sell, throwable) -> {
          if (throwable != null) {
            throwable.printStackTrace();
            PlayerUtils.sendMessage(player, UltraSTS.lang.getErrorSellingPokemon(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
            return;
          }
          Pokemon pokemon = pokemonButtonAction.getPokemon();
          if (Boolean.TRUE.equals(sell)) {
            PlayerUtils.sendMessage(player, UltraSTS.lang.getNotificationSelling()
                .replace("%pokemon%", PokemonUtils.replace(pokemon))
                .replace("%price%", EconomyApi.formatMoney(BigDecimal.valueOf(selectSTS.getFormula().getPokemonValue(pokemonButtonAction.getPokemon())), selectSTS.getEconomy())),
              UltraSTS.lang.getPrefix(),
              TypeMessage.CHAT
            );
            CobbleUtils.server.execute(() -> UIManager.closeUI(player));
          } else {
            PlayerUtils.sendMessage(player, UltraSTS.lang.getCannotSellPokemon(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
          }
        }),
      close -> UltraSTS.lang.getMenu().open(player),
      selectSTS.getBlackList(),
      UltraSTS.lang.getPokemonLore(),
      (pokemon, lore) -> {
        if (lore == null) return;
        List<String> formattedLore = STSSellMenu.buildPokemonLore(selectSTS, pokemon, false);
        lore.clear();
        lore.addAll(formattedLore);
      },
      UltraSTS.lang.getConfirmMenu()
    )).exceptionally(e -> {
      e.printStackTrace();
      return null;
    });
  }
}