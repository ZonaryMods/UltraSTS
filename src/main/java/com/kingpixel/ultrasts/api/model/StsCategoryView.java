package com.kingpixel.ultrasts.api.model;

import com.kingpixel.cobbleutils.Model.ItemModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Headless view model representing an STS category.
 */
public class StsCategoryView {

  private final String id;
  private final String displayName;
  private final ItemModel itemModel;
  private final boolean hasPermission;
  private final boolean onCooldown;
  private final long cooldownRemainingMillis;
  private final String formattedCooldown;
  private final boolean multiSelect;
  private final int maxSelected;
  private final String economyType;
  private final String economyCurrency;
  private final List<String> lore;

  public StsCategoryView(String id,
                         String displayName,
                         ItemModel itemModel,
                         boolean hasPermission,
                         boolean onCooldown,
                         long cooldownRemainingMillis,
                         String formattedCooldown,
                         boolean multiSelect,
                         int maxSelected,
                         String economyType,
                         String economyCurrency,
                         List<String> lore) {
    this.id = id != null ? id : "";
    this.displayName = displayName != null ? displayName : "";
    this.itemModel = itemModel;
    this.hasPermission = hasPermission;
    this.onCooldown = onCooldown;
    this.cooldownRemainingMillis = Math.max(0L, cooldownRemainingMillis);
    this.formattedCooldown = formattedCooldown != null ? formattedCooldown : "";
    this.multiSelect = multiSelect;
    this.maxSelected = maxSelected;
    this.economyType = economyType != null ? economyType : "";
    this.economyCurrency = economyCurrency != null ? economyCurrency : "";
    this.lore = lore != null ? Collections.unmodifiableList(new ArrayList<>(lore)) : Collections.emptyList();
  }

  public String getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public ItemModel getItemModel() {
    return itemModel;
  }

  public boolean hasPermission() {
    return hasPermission;
  }

  public boolean isOnCooldown() {
    return onCooldown;
  }

  public long getCooldownRemainingMillis() {
    return cooldownRemainingMillis;
  }

  public String getFormattedCooldown() {
    return formattedCooldown;
  }

  public boolean isMultiSelect() {
    return multiSelect;
  }

  public int getMaxSelected() {
    return maxSelected;
  }

  public String getEconomyType() {
    return economyType;
  }

  public String getEconomyCurrency() {
    return economyCurrency;
  }

  public List<String> getLore() {
    return lore;
  }

  public boolean isAvailable() {
    return hasPermission && !onCooldown;
  }

  @Override
  public String toString() {
    return "StsCategoryView{" +
      "id='" + id + '\'' +
      ", displayName='" + displayName + '\'' +
      ", hasPermission=" + hasPermission +
      ", onCooldown=" + onCooldown +
      ", cooldownRemainingMillis=" + cooldownRemainingMillis +
      ", multiSelect=" + multiSelect +
      ", maxSelected=" + maxSelected +
      '}';
  }
}
