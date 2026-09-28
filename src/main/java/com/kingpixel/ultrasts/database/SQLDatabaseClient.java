package com.kingpixel.ultrasts.database;

import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobbleutils.util.UtilsFile;
import com.kingpixel.cobbleutils.util.sql.SQLManager;
import com.kingpixel.cobbleutils.util.sql.SQLService;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.database.sql.H2Queries;
import com.kingpixel.ultrasts.database.sql.MySQLQueries;
import com.kingpixel.ultrasts.database.sql.SQLQueries;
import com.kingpixel.ultrasts.database.sql.SQLiteQueries;
import com.kingpixel.ultrasts.models.STS;
import com.kingpixel.ultrasts.models.User;
import com.kingpixel.ultrasts.models.UserOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SQLDatabaseClient extends DatabaseClient {

  private SQLManager sqlManager;
  private SQLQueries sqlQueries;

  private static final Type MONEY_TYPE = new TypeToken<Map<String, BigDecimal>>() {
  }.getType();
  private static final Type COOLDOWN_TYPE = new TypeToken<Map<String, Long>>() {
  }.getType();

  @Override
  public void connect() {
    var config = UltraSTS.config.getDatabase();

    sqlManager = SQLService.getOrCreateManager(config);
    sqlQueries = switch (config.getType()) {
      case SQLITE -> new SQLiteQueries();
      case MYSQL -> new MySQLQueries();
      case MARIADB -> new MySQLQueries();
      case H2 -> new H2Queries();
      default -> throw new IllegalStateException("Unexpected value: " + config.getType());
    };

    try {
      String tableQuery = sqlQueries.createTable();
      if (tableQuery != null && !tableQuery.isBlank()) {
        sqlManager.execute(tableQuery);
      }
      String indexQuery = sqlQueries.createIndex();
      if (indexQuery != null && !indexQuery.isBlank()) {
        sqlManager.execute(indexQuery);
      }
    } catch (Exception e) {
      UltraSTS.LOGGER.error("Failed to create users table", e);
    }
  }

  @Override
  public CompletableFuture<@Nullable User> findUser(@NotNull UUID uuid) {
    User cached = getUser(uuid);

    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    return sqlManager.queryAsync(
      sqlQueries.findUserByUUID(),
      rs -> {
        if (!rs.next()) {
          return null;
        }

        try {
          return parseUser(rs);
        } catch (Exception e) {
          UltraSTS.LOGGER.error(
            "Failed to parse user {} from database",
            uuid,
            e
          );
          return null;
        }
      },
      uuid.toString()
    );
  }

  @Override
  public CompletableFuture<Void> saveUser(@NotNull User user) {
    return sqlManager.withConnectionAsync(conn -> {
      try (PreparedStatement ps = conn.prepareStatement(sqlQueries.upsertUser())) {

        bindUser(ps, user);

        ps.executeUpdate();

        user.setDirty(false);

        // Invalidate leaderboard cache since user data changed
        invalidateAllLeaderboardCache();

      } catch (Exception e) {
        UltraSTS.LOGGER.error(
          "Failed to save user {}",
          user.getUuid(),
          e
        );
      }
    });
  }

  @Override
  public CompletableFuture<Void> saveAll() {
    List<User> users = getSavableUsers();

    if (users.isEmpty()) {
      return CompletableFuture.completedFuture(null);
    }

    // Use native JDBC batch processing which is extremely efficient
    return sqlManager.withConnectionAsync(conn -> {
      try (PreparedStatement ps = conn.prepareStatement(sqlQueries.upsertUser())) {

        for (User user : users) {
          bindUser(ps, user);
          ps.addBatch();
        }

        ps.executeBatch();

        users.forEach(user -> user.setDirty(false));

        // Invalidate leaderboard cache after bulk save
        invalidateAllLeaderboardCache();

      } catch (Exception e) {
        UltraSTS.LOGGER.error("Failed to save users batch", e);
      }
    });
  }

  @Override
  public CompletableFuture<List<User>> findTopUsersImpl(int limit, int page, STS sts) {

    int safeLimit = Math.max(1, limit);
    int safePage = Math.max(1, page);

    int offset = (safePage - 1) * safeLimit;

    String jsonPath = "$.\"" + sts.getId() + "\"";

    return sqlManager.queryListAsync(
      sqlQueries.findTopUsers(),
      rs -> {
        try {
          return parseUser(rs);
        } catch (Exception e) {
          UltraSTS.LOGGER.warn(
            "Failed to parse leaderboard user for STS {}",
            sts.getId(),
            e
          );

          return null;
        }
      },
      jsonPath,
      jsonPath,
      safeLimit,
      offset
    ).thenApply(users ->
      users.stream()
        .filter(Objects::nonNull)
        .toList()
    );
  }

  private void bindUser(PreparedStatement ps, User user) throws Exception {

    ps.setString(1, user.getUuid().toString());
    ps.setString(2, user.getUsername());

    ps.setString(
      3,
      UtilsFile.getGson().toJson(user.getOptions())
    );

    ps.setString(
      4,
      UtilsFile.getGson().toJson(user.getMoneyGained())
    );

    ps.setString(
      5,
      UtilsFile.getGson().toJson(user.getCooldowns())
    );
  }

  private User parseUser(ResultSet rs) throws Exception {

    UUID uuid = UUID.fromString(rs.getString("uuid"));

    String username = rs.getString("username");

    UserOptions options = UtilsFile.getGson().fromJson(
      rs.getString("options"),
      UserOptions.class
    );

    Map<String, BigDecimal> money = UtilsFile.getGson().fromJson(
      rs.getString("money_gained"),
      MONEY_TYPE
    );

    Map<String, Long> cooldowns = UtilsFile.getGson().fromJson(
      rs.getString("cooldowns"),
      COOLDOWN_TYPE
    );

    return User.builder()
      .uuid(uuid)
      .username(username)
      .options(options != null
        ? options
        : new UserOptions())
      .moneyGained(money != null
        ? new ConcurrentHashMap<>(money)
        : new ConcurrentHashMap<>())
      .cooldowns(cooldowns != null
        ? new ConcurrentHashMap<>(cooldowns)
        : new ConcurrentHashMap<>())
      .build();
  }
}