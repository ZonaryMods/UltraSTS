package com.kingpixel.ultrasts.models;

import ca.landonjw.gooeylibs2.api.button.RateLimitedButton;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.*;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.gui.STSMenu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.server.network.ServerPlayerEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class STS {
  private transient String id;
  @Builder.Default
  private boolean multiSelect = true;
  @Builder.Default
  private int maxSelected = 6;
  @Builder.Default
  private ItemModel display = ItemModel.builder()
    .slot(1)
    .item("minecraft:diamond_sword")
    .displayname("&bSTS")
    .lore(List.of(
      "&7Click to enter the category STS",
      "&7Cooldown: %cooldown%",
      "&7Can Join: %canjoin%"
    ))
    .build();
  @Builder.Default
  private EconomyUse economy = new EconomyUse("IMPACTOR", "impactor:dollars");
  @Builder.Default
  private DurationValue cooldown = DurationValue.parse("5m");
  @Builder.Default
  private Map<String, DurationValue> cooldownPermissions = Map.of(
    "ultrasts.cooldown.reduce", DurationValue.parse("1m"),
    "ultrasts.cooldown.bypass", DurationValue.parse("1m")
  );
  @Builder.Default
  private String permission = "";
  @Builder.Default
  private PokemonFormula formula = new PokemonFormula();
  @Builder.Default
  private PokemonBlackList blackList = new PokemonBlackList();

  @Nullable
  public RateLimitedButton getButton(ServerPlayerEntity player) {
    User user = UltraSTS.database.getUser(player.getUuid());
    if (user == null) return null;
    List<String> lore = display != null && display.getLore() != null ? new ArrayList<>(display.getLore()) : new ArrayList<>();
    lore.replaceAll(s -> s
      .replace("%cooldown%", PlayerUtils.getCooldown(user.getCooldown(this)))
      .replace("%canjoin%", user.hasPermission(player, this) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())
    );
    return display.getButton(1, null, lore, action -> {
      if (!user.hasPermission(player, this)) {
        PlayerUtils.sendMessage(player, UltraSTS.lang.getNoPermissionCategory().replace("%permission%", getPermissionRequired()), UltraSTS.lang.getPrefix());
        return;
      }
      STSMenu.open(player, this);
    }, 1, TimeUnit.SECONDS, 1);
  }

  public String getPermissionRequired() {
    if (permission != null && !permission.isEmpty()) {
      return permission;
    }
    return "ultrasts.join." + id;
  }

  public boolean isBlackListed(Pokemon pokemon) {
    if (pokemon == null) return true;
    return UltraSTS.config.isItemBanned(pokemon) || blackList.isBlackListed(pokemon);
  }
}
