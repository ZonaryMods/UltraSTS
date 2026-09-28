package com.kingpixel.ultrasts;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.UtilsLogger;
import com.kingpixel.cobbleutils.util.async.AsyncContext;
import com.kingpixel.cobbleutils.util.async.UtilsAsync;
import com.kingpixel.ultrasts.commands.Commands;
import com.kingpixel.ultrasts.configs.Config;
import com.kingpixel.ultrasts.configs.Lang;
import com.kingpixel.ultrasts.configs.STSConf;
import com.kingpixel.ultrasts.database.DatabaseClient;
import com.kingpixel.ultrasts.database.DatabaseFactory;
import com.kingpixel.ultrasts.models.User;
import com.kingpixel.ultrasts.tasks.Tasks;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * @author Carlos Varas Alonso - 28/04/2024 23:50
 */
public class UltraSTS implements ModInitializer {
  public static final String MOD_ID = "ultrasts";
  public static final String MOD_NAME = "UltraSTS";
  private static final Path PATH = CobbleUtils.getPath().resolve(MOD_ID);
  public static DatabaseClient database;
  public static final Logger LOGGER = UtilsLogger.getLogger(MOD_ID);

  public static Config config;
  public static Lang lang;

  public static final AsyncContext ASYNC = UtilsAsync.createContext(MOD_ID, MOD_NAME, 1, 1);

  public static AsyncContext getAsyncContext() {
    return ASYNC;
  }

  @Override
  public void onInitialize() {
    LOGGER.info("Initializing UltraSTS...");
    events();
    reload();
  }

  public static void reload() {
    files();
 
    // CORRECCIÓN: Evita fugas de memoria y conexiones duplicadas al hacer /reload
    if (database != null) {
      try {
        LOGGER.info("Closing existing database connection before reloading...");
        database.disconnect();
      } catch (Exception e) {
        LOGGER.error("Error closing old database client during reload", e);
      }
    }

    database = DatabaseFactory.init();
    database.connect();
  }

  public static void files() {
    Config.init();
    Lang.init();
    STSConf.init();
  }

  public static void events() {
    PlayerEvent.PLAYER_JOIN.register(player -> database.findUser(player)
      .whenComplete((user, throwable) -> {
        if (throwable != null) {
          LOGGER.error("An error occurred while loading user data for player " + player.getName().getString(), throwable);
          return;
        }
        if (user == null) user = new User(player);
        user.fix(player);
        user.save();
        DatabaseClient.USERS.put(player.getUuid(), user);
      })
    );

    // CORRECCIÓN: Espera a que termine el guardado asíncrono ANTES de borrar la caché
    PlayerEvent.PLAYER_QUIT.register(player -> {
      if (lang != null && lang.getStsSellMenu() != null) {
        lang.getStsSellMenu().clearSession(player.getUuid());
      }
      User user = database.getUser(player);
      if (user != null) {
        user.save().whenComplete((v, throwable) -> {
          if (throwable != null) {
            LOGGER.error("An error occurred while saving data for quitting player: " + player.getName().getString(), throwable);
          }
          // Se invalida de la caché local únicamente cuando el guardado fue exitoso
          DatabaseClient.USERS.invalidate(player.getUuid());
        });
      }
    });

    LifecycleEvent.SERVER_STARTED.register(evt -> Tasks.register());

    // NOTA: Aquí se llama al .join() interno de disconnect(), deteniendo el hilo de
    // Minecraft el tiempo justo y necesario para asegurar que ningún dato se pierda en el apagado.
    LifecycleEvent.SERVER_STOPPING.register(server -> {
      if (database != null) {
        LOGGER.info("Saving all pending data and disconnecting from database...");
        database.disconnect();
      }
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> Commands.register(dispatcher));
  }

  public static Path getPath() {
    return PATH;
  }
}