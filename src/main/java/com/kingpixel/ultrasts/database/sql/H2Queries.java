package com.kingpixel.ultrasts.database.sql;

public class H2Queries implements SQLQueries {

  @Override
  public String createTable() {
    return """
          CREATE TABLE IF NOT EXISTS users (
              uuid VARCHAR(36) PRIMARY KEY,
              username VARCHAR(32) NOT NULL,
              options CLOB NOT NULL,
              money_gained CLOB NOT NULL,
              cooldowns CLOB NOT NULL,
              buyback CLOB NOT NULL
          );
      """;
  }

  @Override
  public String createIndex() {
    return """
          CREATE INDEX IF NOT EXISTS idx_username
          ON users(username);
      """;
  }

  @Override
  public String upsertUser() {
    return """
          MERGE INTO users (uuid, username, options, money_gained, cooldowns, buyback)
          KEY(uuid)
          VALUES (?, ?, ?, ?, ?, ?)
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
          ORDER BY CAST(JSON_VALUE(money_gained, ?) AS DOUBLE) DESC
          LIMIT ? OFFSET ?
      """;
  }
}