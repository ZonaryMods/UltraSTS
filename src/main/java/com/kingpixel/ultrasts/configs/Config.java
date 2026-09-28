package com.kingpixel.ultrasts.configs;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.DataBaseType;
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultrasts.UltraSTS;
import lombok.Data;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@Data
public class Config {
  private boolean debug = false;
  private String lang = "en_us";
  private List<String> commands = List.of(UltraSTS.MOD_ID, "sts");
  private Set<String> blockHeldItems = Set.of(
    "minecraft:diamond"
  );
  private DataBaseConfig database = DataBaseConfig.builder()
    .database(UltraSTS.MOD_ID)
    .type(DataBaseType.JSON)
    .build();
  private DurationValue notificationCooldown = DurationValue.parse("15m");

  public static void init() {
    Path file = UltraSTS.getPath().resolve("config.json");
    try {
      Config config = UtilsFile.read(file, Config.class);
      if (config == null) config = new Config();
      config.fix();
      UltraSTS.config = config;
      UtilsFile.writeAsync(file, config);
    } catch (IOException e) {
      UltraSTS.LOGGER.error("Failed to load or save config.json", e);
    }
  }

  private void fix() {
    if (commands == null) commands = List.of(UltraSTS.MOD_ID, "sts");
    if (blockHeldItems == null) blockHeldItems = Set.of("minecraft:diamond");
    if (notificationCooldown == null) notificationCooldown = DurationValue.parse("15m");
  }

  public boolean isItemBanned(Pokemon pokemon) {
    ItemStack itemStack = pokemon.heldItemNoCopy$common();
    if (itemStack.isEmpty()) return false;
    Item item = itemStack.getItem();
    Identifier id = Registries.ITEM.getId(item);
    String fullId = id.toString();
    String path = id.getPath();
    return blockHeldItems.contains("*") || blockHeldItems.contains(fullId) || blockHeldItems.contains(path);
  }

}
