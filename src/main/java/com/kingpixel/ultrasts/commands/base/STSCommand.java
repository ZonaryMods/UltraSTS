package com.kingpixel.ultrasts.commands.base;

import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.ultrasts.UltraSTS;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.gui.STSMenu;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class STSCommand {

  public static void register(LiteralArgumentBuilder<ServerCommandSource> base) {
    base
      .requires(source -> PermissionApi.hasPermission(source, "ultrasts.user", 2))
      .executes(context -> {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        if (player == null) return 0;
        UltraSTS.lang.getMenu().open(player);
        return 1;
      }).then(
        CommandManager.literal("buyback")
          .requires(source -> UltraSTS.config.getBuyback() != null && UltraSTS.config.getBuyback().hasPermission(source))
          .executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            if (player == null) return 0;
            if (!UltraSTS.config.getBuyback().isEnabled()) {
              PlayerUtils.sendMessage(player, UltraSTS.lang.getBuybackDisabled(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
              return 0;
            }
            UltraSTS.lang.getBuybackMenu().open(player, 0);
            return 1;
          })
      ).then(
        CommandManager.literal("recover")
          .requires(source -> UltraSTS.config.getBuyback() != null && UltraSTS.config.getBuyback().hasPermission(source))
          .executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            if (player == null) return 0;
            if (!UltraSTS.config.getBuyback().isEnabled()) {
              PlayerUtils.sendMessage(player, UltraSTS.lang.getBuybackDisabled(), UltraSTS.lang.getPrefix(), TypeMessage.CHAT);
              return 0;
            }
            UltraSTS.lang.getBuybackMenu().open(player, 0);
            return 1;
          })
      ).then(
        CommandManager.literal("other")
          .then(
            CommandManager.argument("player", EntityArgumentType.player())
              .requires(source -> PermissionApi.hasPermission(source, "ultrasts.admin", 2))
              .executes(context -> {
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (target == null) return 0;
                UltraSTS.lang.getMenu().open(target);
                return 1;
              }).then(
                CommandManager.argument("sts", StringArgumentType.string())
                  .suggests((context, builder) -> {
                    for (String sts : STSConf.STS_MAP.keySet()) {
                      builder.suggest(sts);
                    }
                    return builder.buildFuture();
                  })
                  .executes(context -> {
                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                    if (target == null) return 0;
                    String sts = StringArgumentType.getString(context, "sts");
                    STSMenu.open(target, STSConf.getSTS(sts));
                    return 1;
                  })
              )
          )
      );
  }
}
