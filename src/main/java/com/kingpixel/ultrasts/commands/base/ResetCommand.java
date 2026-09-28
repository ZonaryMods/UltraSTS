package com.kingpixel.ultrasts.commands.base;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.models.User;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ResetCommand {
  private ResetCommand() {}

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base.then(
      CommandManager.literal("reset")
        .requires(source -> PermissionApi.hasPermission(source, "ultrasts.admin", 2))
        .executes(context -> {
          ServerPlayerEntity player = context.getSource().getPlayer();
          if (player == null) {
            context.getSource().sendMessage(AdventureTranslator.toNative(
              UltraSTS.lang.getPrefix() + "Usage: /sts reset <player>"
            ));
            return 0;
          }
          User user = UltraSTS.database.getUser(player);
          if (user == null) return 0;
          user.reset();
          user.save();
          context.getSource().sendMessage(AdventureTranslator.toNative(
            UltraSTS.lang.getResetSuccess()
          ));
          return 1;
        }).then(
          CommandManager.argument("player", StringArgumentType.string())
            .requires(source -> PermissionApi.hasPermission(source, "ultrasts.admin", 2))
            .executes(context -> {
              String targetName = StringArgumentType.getString(context, "player");
              var userCache = CobbleUtils.server.getUserCache();
              if (userCache == null) return 0;
              var byName = userCache.findByName(targetName);
              if (byName.isEmpty()) {
                context.getSource().sendMessage(AdventureTranslator.toNative(
                  UltraSTS.lang.getPlayerNotFound().replace("%player%", targetName)
                ));
                return 0;
              }
              UltraSTS.database.findUser(byName.get().getId())
                .whenComplete((user, throwable) -> {
                  if (throwable != null) {
                    UltraSTS.LOGGER.error("Error finding user for reset", throwable);
                    return;
                  }
                  if (user == null) {
                    context.getSource().sendMessage(AdventureTranslator.toNative(
                      UltraSTS.lang.getPlayerNotFound().replace("%player%", targetName)
                    ));
                    return;
                  }
                  user.reset();
                  user.save();
                  context.getSource().sendMessage(AdventureTranslator.toNative(
                    UltraSTS.lang.getResetSuccessOther().replace("%player%", targetName)
                  ));
                });
              return 1;
            })
        )
    );
  }
}
