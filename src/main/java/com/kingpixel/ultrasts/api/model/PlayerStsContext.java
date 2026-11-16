package com.kingpixel.ultrasts.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Headless view model encapsulating the complete STS context for a player.
 */
public class PlayerStsContext {

  private final UUID playerUuid;
  private final String playerName;
  private final List<StsCategoryView> categories;
  private final List<StsBuybackEntryView> buybackEntries;
  private final Map<String, String> earningsPerCategory;
  private final boolean hasAnyAvailableCategory;
  private final boolean buybackEnabled;

  public PlayerStsContext(UUID playerUuid,
                          String playerName,
                          List<StsCategoryView> categories,
                          List<StsBuybackEntryView> buybackEntries,
                          Map<String, String> earningsPerCategory,
                          boolean hasAnyAvailableCategory,
                          boolean buybackEnabled) {
    this.playerUuid = playerUuid;
    this.playerName = playerName != null ? playerName : "";
    this.categories = categories != null ? Collections.unmodifiableList(new ArrayList<>(categories)) : Collections.emptyList();
    this.buybackEntries = buybackEntries != null ? Collections.unmodifiableList(new ArrayList<>(buybackEntries)) : Collections.emptyList();
    this.earningsPerCategory = earningsPerCategory != null ? Collections.unmodifiableMap(earningsPerCategory) : Collections.emptyMap();
    this.hasAnyAvailableCategory = hasAnyAvailableCategory;
    this.buybackEnabled = buybackEnabled;
  }

  public UUID getPlayerUuid() {
    return playerUuid;
  }

  public String getPlayerName() {
    return playerName;
  }

  public List<StsCategoryView> getCategories() {
    return categories;
  }

  public List<StsBuybackEntryView> getBuybackEntries() {
    return buybackEntries;
  }

  public Map<String, String> getEarningsPerCategory() {
    return earningsPerCategory;
  }

  public boolean hasAnyAvailableCategory() {
    return hasAnyAvailableCategory;
  }

  public boolean isBuybackEnabled() {
    return buybackEnabled;
  }

  public Optional<StsCategoryView> getCategory(String stsId) {
    if (stsId == null) return Optional.empty();
    return categories.stream()
      .filter(c -> c.getId().equalsIgnoreCase(stsId))
      .findFirst();
  }

  public Optional<StsBuybackEntryView> findBuybackEntry(UUID entryId) {
    if (entryId == null) return Optional.empty();
    return buybackEntries.stream()
      .filter(e -> entryId.equals(e.getEntryId()))
      .findFirst();
  }

  @Override
  public String toString() {
    return "PlayerStsContext{" +
      "playerUuid=" + playerUuid +
      ", playerName='" + playerName + '\'' +
      ", categoryCount=" + categories.size() +
      ", buybackCount=" + buybackEntries.size() +
      ", hasAnyAvailable=" + hasAnyAvailableCategory +
      ", buybackEnabled=" + buybackEnabled +
      '}';
  }
}
