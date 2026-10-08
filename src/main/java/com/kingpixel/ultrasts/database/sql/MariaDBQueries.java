package com.kingpixel.ultrasts.database.sql;

public class MariaDBQueries implements SQLQueries {

  @Override
  public String createTable() {
    return """
          CREATE TABLE IF NOT EXISTS users (
              uuid CHAR(36) NOT NULL PRIMARY KEY,
              username VARCHAR(32) NOT NULL,
              options LONGTEXT NOT NULL,
              money_gained LONGTEXT NOT NULL,
              cooldowns LONGTEXT NOT NULL,
              buyback LONGTEXT NOT NULL,
              INDEX idx_username (username)
          ) ENGINE=InnoDB
          DEFAULT CHARSET=utf8mb4
          COLLATE=utf8mb4_unicode_ci;
      """;
  }

  @Override
  public String createIndex() {
    return "";
  }

  @Override
  public String upsertUser() {
    return """
          INSERT INTO users (uuid, username, options, money_gained, cooldowns, buyback)
          VALUES (?, ?, ?, ?, ?, ?)
          ON DUPLICATE KEY UPDATE
              username = VALUES(username),
              options = VALUES(options),
              money_gained = VALUES(money_gained),
              cooldowns = VALUES(cooldowns),
              buyback = VALUES(buyback)
      """;
  }

  @Override
  public String findUserByUUID() {
    return "SELECT * FROM users WHERE uuid = ?";
  }

  @Override
  public String findTopUsers() {
    return """
          SELECT *
          FROM users
          WHERE JSON_EXTRACT(money_gained, ?) IS NOT NULL
          ORDER BY CAST(JSON_UNQUOTE(JSON_EXTRACT(money_gained, ?)) AS DECIMAL(20,2)) DESC
          LIMIT ? OFFSET ?
      """;
  }
}