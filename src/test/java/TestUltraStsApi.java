import com.kingpixel.ultrasts.api.action.StsActionResult;
import com.kingpixel.ultrasts.api.model.PlayerStsContext;
import com.kingpixel.ultrasts.api.model.StsBuybackEntryView;
import com.kingpixel.ultrasts.api.model.StsCategoryView;
import com.kingpixel.ultrasts.api.ui.StsMenuProvider;
import com.kingpixel.ultrasts.api.ui.StsMenuRegistry;
import com.kingpixel.ultrasts.application.StsActionService;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUltraStsApi {

  @AfterEach
  void tearDown() {
    StsMenuRegistry.unregister();
  }

  @Test
  void testActionResultSuccess() {
    BigDecimal payout = BigDecimal.valueOf(5000);
    StsActionResult result = StsActionResult.successSell("Sold 2 pokemon", "category_a", payout, 2);
    assertTrue(result.isSuccess());
    assertEquals(StsActionResult.Status.SUCCESS, result.getStatus());
    assertEquals("category_a", result.getStsId());
    assertEquals(payout, result.getAmount());
    assertEquals(2, result.getAffectedCount());
    assertEquals(0L, result.getCooldownRemainingMillis());
  }

  @Test
  void testActionResultCooldown() {
    StsActionResult result = StsActionResult.cooldown("On cooldown", "default", 120000L);
    assertFalse(result.isSuccess());
    assertEquals(StsActionResult.Status.ON_COOLDOWN, result.getStatus());
    assertEquals("default", result.getStsId());
    assertEquals(120000L, result.getCooldownRemainingMillis());
    assertEquals(0, result.getAffectedCount());
  }

  @Test
  void testActionResultFailures() {
    StsActionResult notFound = StsActionResult.failure(StsActionResult.Status.STS_NOT_FOUND, "Not found", "missing");
    assertFalse(notFound.isSuccess());
    assertEquals(StsActionResult.Status.STS_NOT_FOUND, notFound.getStatus());
    assertEquals("missing", notFound.getStsId());

    StsActionResult denied = StsActionResult.failure(StsActionResult.Status.PERMISSION_DENIED, "No perm");
    assertFalse(denied.isSuccess());
    assertEquals(StsActionResult.Status.PERMISSION_DENIED, denied.getStatus());
  }

  @Test
  void testMenuRegistryRegistrationAndFallback() {
    assertFalse(StsMenuRegistry.hasCustomProvider());

    StsMenuProvider custom = new StsMenuProvider() {
      @Override public void openMainMenu(ServerPlayerEntity player) {}
      @Override public void openCategoryMenu(ServerPlayerEntity player) {}
      @Override public void openSellMenu(ServerPlayerEntity player, String stsId) {}
      @Override public void openBuybackMenu(ServerPlayerEntity player, int page) {}
      @Override public void openProfileMenu(ServerPlayerEntity player) {}
      @Override public void openLeaderboardMenu(ServerPlayerEntity player) {}
    };

    StsMenuRegistry.register(custom);
    assertTrue(StsMenuRegistry.hasCustomProvider());
    assertEquals(custom, StsMenuRegistry.getProvider());

    // Registering null reverts to default
    StsMenuRegistry.register(null);
    assertFalse(StsMenuRegistry.hasCustomProvider());

    // Re-register and unregister
    StsMenuRegistry.register(custom);
    assertTrue(StsMenuRegistry.hasCustomProvider());
    StsMenuRegistry.unregister();
    assertFalse(StsMenuRegistry.hasCustomProvider());
  }

  @Test
  void testStsCategoryView() {
    List<String> lore = List.of("Line 1", "Line 2");
    StsCategoryView view = new StsCategoryView(
      "shinies",
      "Shiny STS",
      null,
      true,
      false,
      0L,
      "Ready",
      true,
      6,
      "IMPACTOR",
      "dollars",
      lore
    );

    assertEquals("shinies", view.getId());
    assertEquals("Shiny STS", view.getDisplayName());
    assertTrue(view.hasPermission());
    assertFalse(view.isOnCooldown());
    assertEquals(0L, view.getCooldownRemainingMillis());
    assertEquals("Ready", view.getFormattedCooldown());
    assertTrue(view.isMultiSelect());
    assertEquals(6, view.getMaxSelected());
    assertEquals("IMPACTOR", view.getEconomyType());
    assertEquals("dollars", view.getEconomyCurrency());
    assertEquals(2, view.getLore().size());
    assertTrue(view.isAvailable());
  }

  @Test
  void testStsBuybackEntryView() {
    UUID id = UUID.randomUUID();
    BigDecimal original = BigDecimal.valueOf(1000);
    BigDecimal buyback = BigDecimal.valueOf(1200);

    StsBuybackEntryView view = new StsBuybackEntryView(
      id,
      null,
      "Charizard",
      50,
      true,
      "default",
      original,
      buyback,
      "$1,200",
      System.currentTimeMillis(),
      System.currentTimeMillis() + 60000L,
      60000L,
      "1m",
      false
    );

    assertEquals(id, view.getEntryId());
    assertEquals("Charizard", view.getPokemonName());
    assertEquals(50, view.getPokemonLevel());
    assertTrue(view.isShiny());
    assertEquals("default", view.getStsId());
    assertEquals(original, view.getOriginalPrice());
    assertEquals(buyback, view.getBuybackPrice());
    assertEquals("$1,200", view.getFormattedBuybackPrice());
    assertFalse(view.isExpired());
  }

  @Test
  void testPlayerStsContext() {
    UUID uuid = UUID.randomUUID();

    StsCategoryView cat1 = new StsCategoryView("cat1", "Cat 1", null, true, false, 0L, "Ready", true, 6, "IMPACTOR", "dollars", List.of());
    StsCategoryView cat2 = new StsCategoryView("cat2", "Cat 2", null, true, true, 5000L, "5s", false, 1, "VAULT", "coins", List.of());

    UUID entryId = UUID.randomUUID();
    StsBuybackEntryView buyback = new StsBuybackEntryView(entryId, null, "Pikachu", 15, false, "cat1", BigDecimal.TEN, BigDecimal.valueOf(12), "$12", 0L, 0L, 0L, "0s", false);

    PlayerStsContext ctx = new PlayerStsContext(
      uuid,
      "TrainerRed",
      List.of(cat1, cat2),
      List.of(buyback),
      Collections.emptyMap(),
      true,
      true
    );

    assertEquals(uuid, ctx.getPlayerUuid());
    assertEquals("TrainerRed", ctx.getPlayerName());
    assertEquals(2, ctx.getCategories().size());
    assertEquals(1, ctx.getBuybackEntries().size());
    assertTrue(ctx.hasAnyAvailableCategory());
    assertTrue(ctx.isBuybackEnabled());

    Optional<StsCategoryView> foundCat = ctx.getCategory("cat1");
    assertTrue(foundCat.isPresent());
    assertEquals("cat1", foundCat.get().getId());

    Optional<StsBuybackEntryView> foundBuyback = ctx.findBuybackEntry(entryId);
    assertTrue(foundBuyback.isPresent());
    assertEquals("Pikachu", foundBuyback.get().getPokemonName());
  }

  @Test
  void testActionServiceNullSafety() throws ExecutionException, InterruptedException {
    StsActionService service = new StsActionService();

    StsActionResult r1 = service.sellPokemon(null, "id", null).get();
    assertNotNull(r1);
    assertEquals(StsActionResult.Status.ERROR, r1.getStatus());

    StsActionResult r2 = service.sellPokemons(null, "id", null).get();
    assertNotNull(r2);
    assertEquals(StsActionResult.Status.ERROR, r2.getStatus());

    StsActionResult r3 = service.buybackPokemon(null, null).get();
    assertNotNull(r3);
    assertEquals(StsActionResult.Status.ERROR, r3.getStatus());

    StsActionResult r4 = service.resetCooldowns(null).get();
    assertNotNull(r4);
    assertEquals(StsActionResult.Status.ERROR, r4.getStatus());
  }
}
