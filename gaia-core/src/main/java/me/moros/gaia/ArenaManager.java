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

package me.moros.gaia;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import me.moros.gaia.api.Arena;
import me.moros.gaia.api.GaiaChunk;
import me.moros.gaia.api.GaiaRegion;
import me.moros.gaia.api.GaiaUser;
import me.moros.gaia.event.ArenaAnalyzeEvent;
import me.moros.gaia.event.ArenaRevertEvent;
import me.moros.gaia.io.GaiaIO;
import me.moros.gaia.locale.Message;
import me.moros.gaia.util.metadata.ArenaMetadata;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ArenaManager implements Iterable<Arena> {
  private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

  protected final GaiaPlugin plugin;

  public ArenaManager(GaiaPlugin plugin) {
    this.plugin = plugin;
  }

  public Optional<Arena> arena(String name) {
    return Optional.ofNullable(arenas.get(name));
  }

  public boolean contains(String name) {
    return arenas.containsKey(name) || GaiaIO.instance().arenaFileExists(name);
  }

  public List<String> sortedNames() {
    return arenas.keySet().stream().sorted().collect(Collectors.toList());
  }

  public Stream<Arena> stream() {
    return arenas.values().stream();
  }

  public int size() {
    return arenas.size();
  }

  public void add(Arena arena) {
    arenas.putIfAbsent(arena.name(), arena);
  }

  public boolean remove(String name) {
    Arena arena = arenas.remove(name);
    if (arena != null) {
      arena.forEach(plugin.chunkManager()::cancel);
    }
    return GaiaIO.instance().deleteArena(name); // Cleanup files
  }

  public void cancelRevert(Arena arena) {
    arena.reverting(false);
    arena.forEach(GaiaChunk::cancelReverting);
  }

  public void revert(Audience user, Arena arena) {
    long startTime = System.currentTimeMillis();
    arena.reverting(true);
    arena.forEach(gcr -> plugin.chunkManager().revert(gcr, arena.world()));
    plugin.executor().sync().repeat(task -> {
      if (!arena.reverting()) {
        final long deltaTime = System.currentTimeMillis() - startTime;
        Message.CANCEL_SUCCESS.send(user, arena.displayName());
        task.cancel();
        WorldEdit.getInstance().getEventBus().post(new ArenaRevertEvent(arena, deltaTime, true));
      } else {
        if (arena.stream().noneMatch(GaiaChunk::reverting)) {
          final long deltaTime = System.currentTimeMillis() - startTime;
          Message.FINISHED_REVERT.send(user, arena.displayName(), deltaTime);
          arena.reverting(false);
          task.cancel();
          WorldEdit.getInstance().getEventBus().post(new ArenaRevertEvent(arena, deltaTime));
        }
      }
    }, 1, 1);
  }

  public boolean create(GaiaUser user, String arenaName) {
    Player player = plugin.adapt(user);
    Key worldId = user.worldKey();
    if (player == null || worldId == null) {
      return false;
    }
    final Region r;
    final World world = player.getWorld();
    try {
      r = WorldEdit.getInstance().getSessionManager().get(player).getSelection(world);
    } catch (IncompleteRegionException e) {
      Message.CREATE_ERROR_SELECTION.send(user);
      return false;
    }

    if (!(r instanceof CuboidRegion)) {
      Message.CREATE_ERROR_CUBOID.send(user);
      return false;
    }
    int radius = Math.max(Math.max(r.getLength(), r.getWidth()), Math.max(r.getHeight(), 64));
    if (radius > 1024) { // For safety reasons limit to maximum 64 chunks in any direction
      Message.CREATE_ERROR_SIZE.send(user);
      return false;
    }
    if (r.getCenter().distanceSq(player.getLocation().toVector()) > radius * radius) {
      Message.CREATE_ERROR_DISTANCE.send(user);
      return false;
    }

    final BlockVector3 min = BlockVector3.at(r.getMinimumPoint().getX(), r.getMinimumPoint().getY(), r.getMinimumPoint().getZ());
    final BlockVector3 max = BlockVector3.at(r.getMaximumPoint().getX(), r.getMaximumPoint().getY(), r.getMaximumPoint().getZ());
    final GaiaRegion gr = new GaiaRegion(min, max);

    if (stream().filter(a -> a.worldKey().equals(worldId)).map(Arena::region).anyMatch(gr::intersects)) {
      Message.CREATE_ERROR_INTERSECTION.send(user);
      return false;
    }
    final Arena arena = new Arena(arenaName, world, worldId, gr);
    if (!GaiaIO.instance().createArenaFiles(arenaName)) {
      Message.CREATE_ERROR_CRITICAL.send(user);
      return false;
    }
    Message.CREATE_ANALYZING.send(user, arena.displayName());
    long startTime = System.currentTimeMillis();
    if (!splitIntoChunks(arena)) {
      arena.forEach(plugin.chunkManager()::cancel);
      Message.CREATE_FAIL.send(user, arena.displayName());
      return false;
    }
    arena.metadata(new ArenaMetadata(arena));
    final long timeoutMoment = startTime + plugin.configManager().config().timeout();
    plugin.executor().sync().repeat(task -> {
      final long time = System.currentTimeMillis();
      if (time > timeoutMoment) {
        arena.forEach(plugin.chunkManager()::cancel);
        Message.CREATE_FAIL_TIMEOUT.send(user, arena.displayName());
        remove(arena.name());
        task.cancel();
      } else {
        if (arena.stream().allMatch(GaiaChunk::analyzed) && arena.finalizeArena()) {
          WorldEdit.getInstance().getEventBus().post(new ArenaAnalyzeEvent(arena, time - startTime));
          GaiaIO.instance().saveArena(arena).thenAccept(result -> {
            if (result) {
              Message.CREATE_SUCCESS.send(user, arena.displayName());
            } else {
              remove(arena.name());
              Message.CREATE_FAIL.send(user, arena.displayName());
            }
          });
          task.cancel();
        }
      }
    }, 1, 1);
    add(arena);
    return true;
  }

  public long nextRevertTime(Arena arena) {
    return arena.lastReverted() + plugin.configManager().config().cooldown();
  }

  public @Nullable Arena standingArena(GaiaUser user) {
    Key worldId = user.worldKey();
    BlockVector3 point = Optional.ofNullable(user.position()).map(Vector3::toBlockPoint).orElse(null);
    if (worldId == null || point == null) {
      return null;
    }
    Predicate<Arena> matcher = a -> a.worldKey().equals(worldId) && a.region().contains(point);
    return plugin.arenaManager().stream().filter(matcher).findAny().orElse(null);
  }

  public Iterator<Arena> iterator() {
    return Collections.unmodifiableCollection(arenas.values()).iterator();
  }

  protected boolean splitIntoChunks(Arena arena) {
    final int minX = arena.region().min().getX();
    final int maxX = arena.region().max().getX();
    final int minY = arena.region().min().getY();
    final int maxY = arena.region().max().getY();
    final int minZ = arena.region().min().getZ();
    final int maxZ = arena.region().max().getZ();

    int tempX, tempZ;
    BlockVector3 v1, v2;
    for (int x = minX >> 4; x <= maxX >> 4; ++x) {
      tempX = x * 16;
      for (int z = minZ >> 4; z <= maxZ >> 4; ++z) {
        tempZ = z * 16;
        v1 = atXZClamped(tempX, minY, tempZ, minX, maxX, minZ, maxZ);
        v2 = atXZClamped(tempX + 15, maxY, tempZ + 15, minX, maxX, minZ, maxZ);
        final GaiaChunk chunkRegion = new GaiaChunk(UUID.randomUUID(), arena, new GaiaRegion(v1, v2));
        plugin.chunkManager().analyze(chunkRegion, arena.world());
      }
    }
    return arena.amount() > 0;
  }

  private BlockVector3 atXZClamped(int x, int y, int z, int minX, int maxX, int minZ, int maxZ) {
    if (minX > maxX || minZ > maxZ) {
      throw new IllegalArgumentException("Minimum cannot be greater than maximum");
    }
    return BlockVector3.at(Math.max(minX, Math.min(maxX, x)), y, Math.max(minZ, Math.min(maxZ, z)));
  }
}
