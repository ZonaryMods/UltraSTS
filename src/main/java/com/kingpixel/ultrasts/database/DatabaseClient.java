package com.kingpixel.ultrasts.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public abstract class DatabaseClient {
  public static final Cache<UUID, User> USERS = Caffeine.newBuilder()
    .build();

  /**
   * Leaderboard cache with TTL of 45 seconds.
   * Key format: "stsId:limit:page"
   * Invalidated when users are saved.
   */
  public static final Cache<String, List<User>> LEADERBOARD_CACHE = Caffeine.newBuilder()
    .expireAfterWrite(45, TimeUnit.SECONDS)
    .build();

  public abstract void connect();

  /**
   * Disconnects from the database after ensuring all dirty user data
   * cached in memory is successfully saved.
   */
  public void disconnect() {
    try {
      // Block and ensure all "dirty" data is saved before the connection cuts
      saveAll().join();
    } catch (Exception e) {
      UltraSTS.LOGGER.error("Failed to save users during database disconnect", e);
    }
  }

  public @Nullable User getUser(@NotNull ServerPlayerEntity player) {
    return getUser(player.getUuid());
  }

  public @Nullable User getUser(@NotNull UUID uuid) {
    return USERS.getIfPresent(uuid);
  }

  public CompletableFuture<@Nullable User> findUser(@NotNull ServerPlayerEntity player) {
    return findUser(player.getUuid());
  }

  public abstract CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid);

  public abstract CompletableFuture<Void> saveUser(@NotNull User user);

  /**
   * Builds a cache key for leaderboard queries.
   * Format: "stsId:limit:page"
   */
  protected String buildLeaderboardCacheKey(int limit, int page, STS sts) {
    return sts.getId() + ":" + limit + ":" + page;
  }

  /**
   * Invalidates all cached leaderboard entries for a specific STS.
   */
  protected void invalidateLeaderboardCache(STS sts) {
    LEADERBOARD_CACHE.asMap().keySet().removeIf(key -> key.startsWith(sts.getId() + ":"));
  }

  /**
   * Invalidates all cached leaderboard entries.
   */
  protected void invalidateAllLeaderboardCache() {
    LEADERBOARD_CACHE.invalidateAll();
  }

  /**
   * Finds top users for leaderboard with caching.
   * First checks cache, then calls abstract findTopUsersImpl() if needed.
   */
  public CompletableFuture<List<User>> findTopUsers(int limit, int page, STS sts) {
    String cacheKey = buildLeaderboardCacheKey(limit, page, sts);
    List<User> cached = LEADERBOARD_CACHE.getIfPresent(cacheKey);

    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    return findTopUsersImpl(limit, page, sts)
      .thenApply(users -> {
        if (users != null) {
          LEADERBOARD_CACHE.put(cacheKey, users);
        }
        return users;
      });
  }

  /**
   * Abstract method for subclasses to implement the actual leaderboard query.
   */
  protected abstract CompletableFuture<List<User>> findTopUsersImpl(int limit, int page, STS sts);

  public List<User> getSavableUsers() {
    return USERS.asMap().values().stream()
      .filter(User::isDirty)
      .toList();
  }

  public CompletableFuture<Void> saveAll() {
    var savableUsers = getSavableUsers();
    if (savableUsers.isEmpty()) return CompletableFuture.completedFuture(null);

    // Invalidate all leaderboard cache since user data changed
    invalidateAllLeaderboardCache();

    return CompletableFuture.allOf(
      savableUsers.stream()
        .map(User::save)
        .toArray(CompletableFuture[]::new)
    );
  }
}