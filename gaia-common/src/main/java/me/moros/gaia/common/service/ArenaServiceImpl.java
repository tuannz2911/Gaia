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

package me.moros.gaia.common.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.arena.Arena;
import me.moros.gaia.api.arena.Reversible;
import me.moros.gaia.api.locale.Message;
import me.moros.gaia.api.operation.GaiaOperation;
import me.moros.gaia.api.operation.GaiaOperation.Analyze;
import me.moros.gaia.api.operation.GaiaOperation.Revert;
import me.moros.gaia.api.platform.Level;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.api.region.Region;
import me.moros.gaia.api.service.ArenaService;
import me.moros.gaia.api.user.GaiaUser;
import me.moros.gaia.api.util.ChunkUtil;
import me.moros.gaia.api.util.RevertResult;
import me.moros.gaia.common.util.BatchProcessor;
import me.moros.gaia.common.util.FutureUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;

public class ArenaServiceImpl implements ArenaService {
  private final Gaia plugin;

  private final Map<String, Arena> arenas;
  private final Set<String> pendingArenas;

  private final BatchProcessor<Analyze> analyzeBatchProcessor;
  private final BatchProcessor<Revert> revertBatchProcessor;

  public ArenaServiceImpl(Gaia plugin) {
    this.plugin = plugin;
    this.arenas = new ConcurrentHashMap<>();
    this.pendingArenas = ConcurrentHashMap.newKeySet();
    this.analyzeBatchProcessor = new BatchProcessor<>(128);
    this.revertBatchProcessor = new BatchProcessor<>(plugin.configManager().config().concurrentChunks()); // TODO FIX
  }

  @Override
  public boolean contains(String name) {
    return arenas.containsKey(name) || pendingArenas.contains(name) || plugin.coordinator().storage().arenaFileExists(name);
  }

  @Override
  public Optional<Arena> arena(String name) {
    return Optional.ofNullable(arenas.get(name));
  }

  @Override
  public void add(Arena arena) {
    arenas.putIfAbsent(arena.name(), arena);
  }

  @Override
  public boolean remove(String name) {
    Arena arena = arenas.remove(name);
    pendingArenas.remove(name);
    if (arena != null) {
      var level = plugin.coordinator().levelService().findLevel(arena.level());
      if (level != null) {
        arena.forEach(c -> plugin.coordinator().operationManager().cancel(level, c));
      }
    }
    return plugin.coordinator().storage().deleteArena(name); // Cleanup files
  }

  @Override
  public int size() {
    return arenas.size();
  }

  @Override
  public Stream<Arena> stream() {
    return arenas.values().stream();
  }

  @Override
  public Iterator<Arena> iterator() {
    return Collections.unmodifiableCollection(arenas.values()).iterator();
  }

  @Override
  public boolean create(GaiaUser user, String arenaName) {
    var selOpt = plugin.coordinator().userService().selection(user.getOrDefault(Identity.UUID, null));
    if (selOpt.isEmpty()) {
      return false;
    }
    var selection = selOpt.get();
    Level level = plugin.coordinator().levelService().findLevel(selection.level());
    if (level == null) {
      return false;
    }
    //    com.sk89q.worldedit.regions.Region region;
    //    try {
    //      region = WorldEdit.getInstance().getSessionManager().get(player).getSelection(player.getWorld());
    //    } catch (IncompleteRegionException e) {
    //      Message.CREATE_ERROR_SELECTION.send(user);
    //      return false;
    //    }
    //
    //    if (!(region instanceof CuboidRegion)) {
    //      Message.CREATE_ERROR_CUBOID.send(user);
    //      return false;
    //    }
    var region = Region.of(selection.min(), selection.max());
    double radius = region.size().maxComponent();
    if (radius > 1024) { // For safety reasons limit to maximum 64 chunks in any direction
      Message.CREATE_ERROR_SIZE.send(user);
      return false;
    }
    if (region.center().distanceSq(user.position()) > radius * radius) {
      Message.CREATE_ERROR_DISTANCE.send(user);
      return false;
    }
    if (stream().filter(a -> a.level().equals(level.key())).map(Arena::region).anyMatch(region::intersects)) {
      Message.CREATE_ERROR_INTERSECTION.send(user);
      return false;
    }
    if (!plugin.coordinator().storage().createEmptyArenaFiles(arenaName)) {
      Message.CREATE_ERROR_CRITICAL.send(user);
      return false;
    }
    pendingArenas.add(arenaName);
    var chunkRegions = ChunkUtil.splitIntoChunks(region);
    if (chunkRegions.isEmpty()) {
      Message.CREATE_FAIL.send(user, arenaName);
      return false;
    }
    return false;
    /*Message.CREATE_ANALYZING.send(user, arenaName);
    pendingArenas.add(arenaName);
    createFuture(user, arenaName, level, chunkRegions);
    return true;*/
  }

  private void createFuture(Audience user, String arenaName, Level level, Collection<ChunkRegion> chunkRegions) {
    var futures = chunkRegions.stream().map(c -> GaiaOperation.snapshotAnalyze(level, c))
      .map(plugin.coordinator().operationManager()::add).toList();
    var combined = FutureUtil.createFailFastBatch(futures);
    long startTime = System.currentTimeMillis();
    // Extra validation and counting
    Supplier<CompletionException> exSupplier = () -> new CompletionException(new RuntimeException("Unable to save arena"));
    combined.orTimeout(plugin.configManager().config().timeout(), TimeUnit.MILLISECONDS)
      .thenCompose(data -> plugin.coordinator().storage().saveDataAsync(arenaName, data))
      .thenAccept(validated -> {
        if (!validated.isEmpty()) {
          var arena = Arena.builder().name(arenaName).level(level.key()).chunks(validated).build();
          final long time = System.currentTimeMillis();
          plugin.coordinator().eventBus().postArenaAnalyzeEvent(arena, time - startTime);
          Message.CREATE_SUCCESS.send(user, arena.displayName());
        } else {
          throw exSupplier.get();
        }
      }).exceptionally(t -> {
        remove(arenaName);
        if (t instanceof TimeoutException) {
          Message.CREATE_FAIL_TIMEOUT.send(user, arenaName);
        } else {
          Message.CREATE_FAIL.send(user, arenaName);
          plugin.logger().warn(t.getMessage(), t);
        }
        return null;
      });
  }

  @Override
  public RevertResult revert(Arena arena) {
    return RevertResult.unloaded(arena);
    /* TODO fix
    Level level = plugin.coordinator().levelService().findLevel(arena.level());
    if (level == null) {
      return RevertResult.unloaded(arena);
    } else if (arena.reverting()) {
      return RevertResult.reverting(arena);
    }
    arena.resetLastReverted();
    long startTime = System.currentTimeMillis();
    arena.streamChunks().map(GaiaOperation.revert(level, c));
    var future = new CompletableFuture<Void>().handle((v, e) -> {
      boolean completed = e == null;
      long result = completed ? -1 : System.currentTimeMillis() - startTime;
      plugin.coordinator().eventBus().postArenaRevertEvent(arena, result, completed);
      return completed ? OptionalLong.of(result) : OptionalLong.empty();
    });
    return RevertResult.success(arena, future);*/
  }

  @Override
  public void cancelRevert(Arena arena) {
    arena.forEach(this::stopReverting);
  }

  private void stopReverting(Reversible.Mutable reversible) {
    reversible.reverting(false);
  }
}
