package com.kingpixel.ultrasts.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.EconomyApi;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.ultrasts.UltraSTS;
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

  public User(ServerPlayerEntity player) {
    this.uuid = player.getUuid();
    this.username = player.getName().getString();
    this.options = new UserOptions();
    this.cooldowns = new ConcurrentHashMap<>();
    this.moneyGained = new ConcurrentHashMap<>();
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
    return cooldowns.values().stream().anyMatch(expiresAt -> expiresAt <= now);
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
