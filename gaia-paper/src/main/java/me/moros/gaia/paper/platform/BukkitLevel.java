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

package me.moros.gaia.paper.platform;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.chunk.ChunkPosition;
import me.moros.gaia.api.platform.Level;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.common.platform.GaiaChunkData;
import me.moros.gaia.common.util.DataIterator;
import net.kyori.adventure.key.Key;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

public record BukkitLevel(World handle, JavaPlugin plugin) implements Level {
  @SuppressWarnings("unchecked")
  @Override
  public boolean restoreSnapshot(ChunkData data, int amount) {
    if (amount > 0 && data instanceof GaiaChunkData<?> chunkData) {
      final int yOffset = chunkData.chunk().region().min().blockY() >> 4;
      final DataIterator<BlockData> it = (DataIterator<BlockData>) chunkData.cachedIterator();
      int counter = 0;
      int index;
      while (it.hasNext() && ++counter <= amount) {
        index = it.index();
        final int y = index >> 8;
        final int remainder = index - (y << 8);
        final int z = remainder >> 4;
        final int x = remainder - z << 4;
        handle().setBlockData(x, yOffset + y, z, it.next());
      }
      return it.hasNext();
    }
    return false;
  }

  @Override
  public CompletableFuture<ChunkData> snapshot(ChunkRegion chunk) {
    return handle().getChunkAtAsync(chunk.x(), chunk.z(), false)
      .thenApply(c -> new BukkitChunkData(chunk, c.getChunkSnapshot(false, false, false)));
  }

  @Override
  public CompletableFuture<?> loadChunkWithTicket(int x, int z) {
    return handle().getChunkAtAsync(x, z, false).thenAccept(c -> c.addPluginChunkTicket(plugin()));
  }

  @Override
  public void removeChunkTicket(int x, int z) {
    handle().removePluginChunkTicket(x, z, plugin());
  }

  @Override
  public void fixLight(Collection<ChunkPosition> positions) {
  }

  @Override
  public @NonNull Key key() {
    return handle().key();
  }
}
