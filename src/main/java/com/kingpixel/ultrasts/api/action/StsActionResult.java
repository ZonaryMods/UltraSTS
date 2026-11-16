package com.kingpixel.ultrasts.api.action;

import java.math.BigDecimal;

/**
 * Result model representing the outcome of an UltraSTS action (sell, buyback, reset).
 */
public class StsActionResult {

  public enum Status {
    SUCCESS,
    STS_NOT_FOUND,
    ON_COOLDOWN,
    PERMISSION_DENIED,
    POKEMON_NOT_FOUND,
    POKEMON_BLACKLISTED,
    BUYBACK_NOT_FOUND,
    BUYBACK_EXPIRED,
    INSUFFICIENT_FUNDS,
    STORAGE_FULL,
    DISABLED,
    ERROR
  }

  private final Status status;
  private final String message;
  private final String stsId;
  private final BigDecimal amount;
  private final long cooldownRemainingMillis;
  private final int affectedCount;

  public StsActionResult(Status status,
                         String message,
                         String stsId,
                         BigDecimal amount,
                         long cooldownRemainingMillis,
                         int affectedCount) {
    this.status = status;
    this.message = message != null ? message : "";
    this.stsId = stsId != null ? stsId : "";
    this.amount = amount != null ? amount : BigDecimal.ZERO;
    this.cooldownRemainingMillis = Math.max(0L, cooldownRemainingMillis);
    this.affectedCount = affectedCount;
  }

  public static StsActionResult success(String message) {
    return new StsActionResult(Status.SUCCESS, message, "", BigDecimal.ZERO, 0L, 1);
  }

  public static StsActionResult successSell(String message, String stsId, BigDecimal amount, int soldCount) {
    return new StsActionResult(Status.SUCCESS, message, stsId, amount, 0L, soldCount);
  }

  public static StsActionResult successBuyback(String message, BigDecimal cost) {
    return new StsActionResult(Status.SUCCESS, message, "", cost, 0L, 1);
  }

  public static StsActionResult cooldown(String message, String stsId, long cooldownRemainingMillis) {
    return new StsActionResult(Status.ON_COOLDOWN, message, stsId, BigDecimal.ZERO, cooldownRemainingMillis, 0);
  }

  public static StsActionResult failure(Status status, String message) {
    return new StsActionResult(status, message, "", BigDecimal.ZERO, 0L, 0);
  }

  public static StsActionResult failure(Status status, String message, String stsId) {
    return new StsActionResult(status, message, stsId, BigDecimal.ZERO, 0L, 0);
  }

  public boolean isSuccess() {
    return status == Status.SUCCESS;
  }

  public Status getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public String getStsId() {
    return stsId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public long getCooldownRemainingMillis() {
    return cooldownRemainingMillis;
  }

  public int getAffectedCount() {
    return affectedCount;
  }

  @Override
  public String toString() {
    return "StsActionResult{" +
      "status=" + status +
      ", message='" + message + '\'' +
      ", stsId='" + stsId + '\'' +
      ", amount=" + amount +
      ", cooldownRemainingMillis=" + cooldownRemainingMillis +
      ", affectedCount=" + affectedCount +
      '}';
  }
}
