/*
 * Copyright 2020-2023 Moros
 *
 * This file is part of Gaia.
 *
 * Gaia is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gaia is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Gaia. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.gaia.fabric;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricServerCommandManager;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import me.moros.gaia.ArenaManager;
import me.moros.gaia.ChunkManager;
import me.moros.gaia.GaiaPlugin;
import me.moros.gaia.api.ArenaPoint;
import me.moros.gaia.api.GaiaUser;
import me.moros.gaia.command.Commander;
import me.moros.gaia.config.ConfigManager;
import me.moros.gaia.io.GaiaIO;
import me.moros.gaia.locale.TranslationManager;
import me.moros.tasker.executor.CompositeExecutor;
import me.moros.tasker.executor.SimpleAsyncExecutor;
import me.moros.tasker.fabric.FabricExecutor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GaiaFabric implements GaiaPlugin {
  private final ModContainer container;
  private final Logger logger;

  private final BlockState AIR;
  private final CompositeExecutor executor;
  private final ParserContext parserContext;
  private final ConfigManager configManager;
  private final Commander commander;
  private final TranslationManager translationManager;
  private final ArenaManager arenaManager;
  private final ChunkManager chunkManager;
  private MinecraftServer server;

  GaiaFabric(ModContainer container, Path path) {
    this.container = container;
    this.logger = LoggerFactory.getLogger(container.getMetadata().getName());
    configManager = new ConfigManager(logger, path);

    int threads = Math.max(8, 2 * configManager.config().concurrentChunks());
    var pool = Executors.newScheduledThreadPool(threads);
    executor = CompositeExecutor.of(new FabricExecutor(), new SimpleAsyncExecutor(pool));
    parserContext = new ParserContext();
    parserContext.setRestricted(false);
    parserContext.setTryLegacy(false);
    parserContext.setPreferringWildcard(false);
    AIR = FabricAdapter.adapt(Blocks.AIR.defaultBlockState());

    translationManager = new TranslationManager(logger, path);

    arenaManager = new ArenaManager(this);
    chunkManager = new FabricChunkManager(this);

    if (!GaiaIO.createInstance(this, path)) {
      throw new RuntimeException("Could not create Arenas folder! Aborting mod load.");
    }
    long startTime = System.currentTimeMillis();
    GaiaIO.instance().loadAllArenas().thenRun(() -> {
      long delta = System.currentTimeMillis() - startTime;
      int size = arenaManager.size();
      logger.info("Successfully loaded " + size + (size == 1 ? " arena" : " arenas") + " (" + delta + "ms)");
    });

    registerLifecycleListeners();
    CommandManager<GaiaUser> manager = new FabricServerCommandManager<>(
      CommandExecutionCoordinator.simpleCoordinator(),
      s -> FabricGaiaUser.from(this, s), s -> ((FabricGaiaUser) s).stack()
    );
    commander = Commander.create(manager, this);
    configManager.save();
  }

  private void registerLifecycleListeners() {
    ServerLifecycleEvents.SERVER_STARTED.register(this::onEnable);
    ServerLifecycleEvents.SERVER_STOPPING.register(this::onDisable);
  }

  private void onEnable(MinecraftServer server) {
    this.server = server;
  }

  private void onDisable(MinecraftServer server) {
    executor.shutdown();
    if (chunkManager != null) {
      chunkManager.shutdown();
    }
    this.server = null;
  }

  @Override
  public String author() {
    return container.getMetadata().getVersion().getFriendlyString();
  }

  @Override
  public String version() {
    return container.getMetadata().getAuthors().stream().map(Person::getName).findFirst().orElse("Moros");
  }

  @Override
  public Logger logger() {
    return logger;
  }

  @Override
  public ConfigManager configManager() {
    return configManager;
  }

  @Override
  public TranslationManager translationManager() {
    return translationManager;
  }

  @Override
  public ArenaManager arenaManager() {
    return arenaManager;
  }

  @Override
  public ChunkManager chunkManager() {
    return chunkManager;
  }

  @Override
  public BlockState parseBlockData(@Nullable String value) {
    if (value != null) {
      try {
        return WorldEdit.getInstance().getBlockFactory().parseFromInput(value, parserContext).toImmutableState();
      } catch (InputParseException e) {
        logger.warn("Invalid BlockState in palette: " + value + ". Block will be replaced with air.");
      }
    }
    return AIR;
  }

  public @Nullable ServerLevel adapt(Key worldKey) {
    for (ServerLevel level : server.getAllLevels()) {
      if (worldKey.equals(level.dimension().location())) {
        return level;
      }
    }
    return null;
  }

  @Override
  public @Nullable World findWorld(Key worldKey) {
    ServerLevel world = adapt(worldKey);
    if (world == null) {
      logger.warn("Couldn't find world with key " + worldKey);
      return null;
    }
    return FabricAdapter.adapt(world);
  }

  @Override
  public @Nullable GaiaUser findUser(String input) {
    var player = server.getPlayerList().getPlayerByName(input);
    if (player == null) {
      try {
        UUID uuid = UUID.fromString(input);
        player = server.getPlayerList().getPlayer(uuid);
      } catch (Exception ignore) {
      }
    }
    return player == null ? null : FabricGaiaUser.from(this, player.createCommandSourceStack());
  }

  @Override
  public Stream<String> users() {
    return Arrays.stream(server.getPlayerList().getPlayerNamesArray());
  }

  @Override
  public CompositeExecutor executor() {
    return executor;
  }

  @Override
  public @Nullable ArenaPoint pointFromUser(GaiaUser user) {
    if (!user.isPlayer()) {
      return null;
    }
    var loc = adapt(user).getLocation();
    return new ArenaPoint(loc.toVector(), loc.getYaw(), loc.getPitch());
  }

  @Override
  public void teleport(GaiaUser user, Key worldKey, ArenaPoint point) {
    var world = adapt(worldKey);
    if (!user.isPlayer() || world == null) {
      return;
    }
    adapt(user).setLocation(new Location(FabricAdapter.adapt(world), point.v(), point.yaw(), point.pitch()));
  }

  @Override
  public Player adapt(GaiaUser user) {
    return user.pointers().get(Identity.UUID)
      .map(server.getPlayerList()::getPlayer)
      .map(FabricAdapter::adaptPlayer)
      .orElseThrow(() -> new IllegalArgumentException("User is not a valid player!"));
  }
}
