package com.kingpixel.ultrasts.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.EconomyUse;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Builder.Default
  private transient AtomicBoolean dirty = new AtomicBoolean(false);
  private UUID uuid;
  private String username;
  @Builder.Default
  private UserOptions options = new UserOptions();
  @Builder.Default
  private Map<String, BigDecimal> moneyGained = new ConcurrentHashMap<>();
  @Builder.Default
  private Map<String, Long> cooldowns = new ConcurrentHashMap<>();
  @Builder.Default
  private List<BuybackEntry> buyback = new CopyOnWriteArrayList<>();

  public User(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.username = player.getName().getString();
    this.options = new UserOptions();
    this.cooldowns = new ConcurrentHashMap<>();
    this.moneyGained = new ConcurrentHashMap<>();
    this.buyback = new CopyOnWriteArrayList<>();
    markDirty();
  }


  public boolean addCooldown(STS sts, ServerPlayerEntity player) {
    return addCooldown(sts, player, 1);
  }

  public boolean addCooldown(STS sts, ServerPlayerEntity player, int amount) {
    if (hasCooldown(sts)) return false;
    String key = sts.getId();
    long duration = PlayerUtils.getCooldown(sts.getCooldownPermissions(), sts.getCooldown(), player);
    duration *= amount;
    long now = System.currentTimeMillis();
    cooldowns.put(key, now + duration);
    markDirty();
    return true;
  }

  public boolean hasSomeWithoutCooldown() {
    long now = System.currentTimeMillis();
    if (STSConf.STS_MAP.isEmpty()) return false;
    for (STS sts : STSConf.STS_MAP.values()) {
      long expiresAt = cooldowns.getOrDefault(sts.getId(), 0L);
      if (expiresAt <= now) {
        return true;
      }
    }
    return false;
  }

  public CompletableFuture<Boolean> sellPokemon(STS sts, Pokemon pokemon, ServerPlayerEntity player) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    CobbleUtils.server.execute(() -> {
      synchronized (this) {
        try {
          if (hasCooldown(sts)) {
            future.complete(false);
            return;
          }
          BigDecimal price = BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
          if (price.compareTo(BigDecimal.ZERO) <= 0) {
            future.complete(false);
            return;
          }

          var party = Cobblemon.INSTANCE.getStorage().getParty(player);
          var pc = Cobblemon.INSTANCE.getStorage().getPC(player);

          if (party.remove(pokemon) || pc.remove(pokemon)) {
            addCooldown(sts, player);
            moneyGained.merge(sts.getId(), price, BigDecimal::add);
            EconomyApi.addMoney(player.getUuid(), price, sts.getEconomy());
            addBuyback(pokemon, price, sts);
            markDirty();
            future.complete(true);
          } else {
            future.complete(false);
          }
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      }
    });
    return future;
  }

  public CompletableFuture<BigDecimal> sellPokemons(STS sts, List<Pokemon> pokemons, ServerPlayerEntity player) {
    CompletableFuture<BigDecimal> future = new CompletableFuture<>();
    CobbleUtils.server.execute(() -> {
      synchronized (this) {
        try {
          if (hasCooldown(sts)) {
            future.complete(BigDecimal.ZERO);
            return;
          }

          var party = Cobblemon.INSTANCE.getStorage().getParty(player);
          var pc = Cobblemon.INSTANCE.getStorage().getPC(player);

          BigDecimal totalPool = BigDecimal.ZERO;
          int actuallyRemovedCount = 0;

          for (Pokemon pokemon : pokemons) {
            BigDecimal price = BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
            if (price.compareTo(BigDecimal.ZERO) > 0 && (party.remove(pokemon) || pc.remove(pokemon))) {
              totalPool = totalPool.add(price);
              actuallyRemovedCount++;
              addBuyback(pokemon, price, sts);
            }
          }

          if (actuallyRemovedCount > 0) {
            addCooldown(sts, player, actuallyRemovedCount);
            moneyGained.merge(sts.getId(), totalPool, BigDecimal::add);
            EconomyApi.addMoney(player.getUuid(), totalPool, sts.getEconomy());
            markDirty();
            future.complete(totalPool);
          } else {
            future.complete(BigDecimal.ZERO);
          }
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      }
    });
    return future;
  }

  public boolean hasCooldown(STS sts) {
    String key = sts.getId();
    long expiresAt = cooldowns.getOrDefault(key, 0L);
    return expiresAt > System.currentTimeMillis();
  }

  public AtomicBoolean getDirty() {
    if (dirty == null) dirty = new AtomicBoolean(false);
    return dirty;
  }

  public void markDirty() {
    getDirty().set(true);
  }

  public CompletableFuture<Void> save() {
    if (dirty.getAndSet(false)) return UltraSTS.database.saveUser(this);
    return CompletableFuture.completedFuture(null);
  }

  public void fix(ServerPlayerEntity player) {
    String name = player.getName().getString();
    if (username == null) {
      username = name;
      markDirty();
    }
    if (!username.equals(name)) {
      username = name;
      markDirty();
    }
    if (options == null) {
      options = new UserOptions();
      markDirty();
    }
    if (moneyGained == null) {
      moneyGained = new ConcurrentHashMap<>();
      markDirty();
    }
    if (cooldowns == null) {
      cooldowns = new ConcurrentHashMap<>();
      markDirty();
    }
    if (buyback == null) {
      buyback = new CopyOnWriteArrayList<>();
      markDirty();
    } else {
      pruneExpiredBuyback();
    }
  }

  public void addBuyback(Pokemon pokemon, BigDecimal price, STS sts) {
    if (UltraSTS.config.getBuyback() == null || !UltraSTS.config.getBuyback().isEnabled()) return;
    if (pokemon == null || price == null || sts == null) return;
    pruneExpiredBuyback();
    if (buyback == null) buyback = new CopyOnWriteArrayList<>();
    int max = UltraSTS.config.getBuyback().getMaxEntriesPerUser();
    if (max > 0 && buyback.size() >= max) {
      while (buyback.size() >= max && !buyback.isEmpty()) {
        buyback.remove(0);
      }
    }
    buyback.add(BuybackEntry.builder()
      .id(UUID.randomUUID())
      .pokemon(pokemon)
      .originalPrice(price)
      .stsId(sts.getId())
      .economy(sts.getEconomy())
      .timestamp(System.currentTimeMillis())
      .build());
    markDirty();
  }

  public void pruneExpiredBuyback() {
    if (buyback == null) {
      buyback = new CopyOnWriteArrayList<>();
      return;
    }
    if (UltraSTS.config.getBuyback() == null) return;
    long maxAgeMillis = UltraSTS.config.getBuyback().getExpiration().toMillis();
    if (maxAgeMillis > 0) {
      boolean removed = buyback.removeIf(entry -> entry == null || entry.isExpired(maxAgeMillis));
      if (removed) {
        markDirty();
      }
    }
  }

  public CompletableFuture<Boolean> buybackPokemon(UUID entryId, ServerPlayerEntity player) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    CobbleUtils.server.execute(() -> {
      synchronized (this) {
        try {
          if (UltraSTS.config.getBuyback() == null || !UltraSTS.config.getBuyback().isEnabled()) {
            future.complete(false);
            return;
          }
          pruneExpiredBuyback();
          BuybackEntry targetEntry = null;
          for (BuybackEntry entry : buyback) {
            if (entry != null && entry.getId() != null && entry.getId().equals(entryId)) {
              targetEntry = entry;
              break;
            }
          }
          if (targetEntry == null) {
            PlayerUtils.sendMessage(player, UltraSTS.lang.getBuybackNotFound(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
            future.complete(false);
            return;
          }
          long maxAgeMillis = UltraSTS.config.getBuyback().getExpiration().toMillis();
          if (targetEntry.isExpired(maxAgeMillis)) {
            buyback.remove(targetEntry);
            markDirty();
            PlayerUtils.sendMessage(player, UltraSTS.lang.getBuybackExpired(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
            future.complete(false);
            return;
          }
          STS sts = STSConf.getSTS(targetEntry.getStsId());
          if (sts == null) {
            sts = STSConf.getSTS("");
          }

          BigDecimal currentMarketPrice = getCurrentMarketPrice(sts, targetEntry.getPokemon());

          BigDecimal buybackPrice = UltraSTS.config.getBuyback().calculateSecureBuybackPrice(
            targetEntry.getOriginalPrice(),
            currentMarketPrice
          );

          EconomyUse economy = targetEntry.getEconomy();
          if (economy == null && sts != null) {
            economy = sts.getEconomy();
          }

          if (!EconomyApi.hasEnoughMoney(player.getUuid(), buybackPrice, economy, false)) {
            PlayerUtils.sendMessage(player,
              UltraSTS.lang.getBuybackNotEnoughMoney()
                .replace("%price%", EconomyApi.formatMoney(buybackPrice, economy)),
              UltraSTS.lang.getPrefix(),
              TypeMessage.CHAT);
            future.complete(false);
            return;
          }

          var party = Cobblemon.INSTANCE.getStorage().getParty(player);
          var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
          Pokemon pokemon = targetEntry.getPokemon();

          BigDecimal originalSoldPrice = targetEntry.getOriginalPrice();
          if (party.add(pokemon) || pc.add(pokemon)) {
            EconomyApi.removeMoney(player.getUuid(), buybackPrice, economy);
            if (targetEntry.getStsId() != null && originalSoldPrice != null) {
              moneyGained.computeIfPresent(targetEntry.getStsId(), (k, v) -> {
                BigDecimal updated = v.subtract(originalSoldPrice);
                return updated.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : updated;
              });
            }
            buyback.remove(targetEntry);
            markDirty();
            PlayerUtils.sendMessage(player,
              UltraSTS.lang.getBuybackSuccess()
                .replace("%pokemon%", PokemonUtils.replace(pokemon))
                .replace("%price%", EconomyApi.formatMoney(buybackPrice, economy)),
              UltraSTS.lang.getPrefix(),
              TypeMessage.CHAT);
            future.complete(true);
          } else {
            PlayerUtils.sendMessage(player, UltraSTS.lang.getBuybackStorageFull(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
            future.complete(false);
          }
        } catch (Exception e) {
          future.completeExceptionally(e);
        }
      }
    });
    return future;
  }

  private BigDecimal getCurrentMarketPrice(STS sts, Pokemon pokemon) {
    if (sts != null && pokemon != null) {
      try {
        return BigDecimal.valueOf(sts.getFormula().getPokemonValue(pokemon));
      } catch (Exception ignored) {
        // Fallback if formula throws
      }
    }
    return BigDecimal.ZERO;
  }


  public long getCooldown(STS selectSTS) {
    String key = selectSTS.getId();
    return cooldowns.getOrDefault(key, 0L);
  }


  public boolean hasPermission(ServerPlayerEntity player, STS selectSTS) {
    return PermissionApi.hasPermission(player, selectSTS.getPermissionRequired(), 2);
  }

  public void removeCooldown(STS selectSTS) {
    String key = selectSTS.getId();
    cooldowns.remove(key);
    markDirty();
  }

  public String getMoneyEarned(STS value) {
    return EconomyApi.formatMoney(moneyGained.getOrDefault(value.getId(), BigDecimal.ZERO), value.getEconomy());
  }

  public Document toDocument() {
    return Document.parse(UtilsFile.getGson().toJson(this));
  }

  public static @NotNull User fromDocument(Document doc) {
    return UtilsFile.getGson().fromJson(doc.toJson(), User.class);
  }

  public boolean isDirty() {
    return getDirty().get();
  }

  public void setDirty(boolean b) {
    getDirty().set(b);
  }

  public void reset() {
    cooldowns.clear();
    markDirty();
  }
}
