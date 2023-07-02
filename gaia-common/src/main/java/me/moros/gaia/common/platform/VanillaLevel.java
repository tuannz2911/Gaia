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

package me.moros.gaia.common.platform;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.platform.Level;
import me.moros.gaia.api.region.ChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public abstract class VanillaLevel implements Level {
  private static final TicketType<ChunkPos> GAIA_TICKET_TYPE = TicketType.create("gaia", Comparator.comparingLong(ChunkPos::toLong));

  private final ServerLevel handle;

  protected VanillaLevel(ServerLevel handle) {
    this.handle = handle;
  }

  protected ServerLevel handle() {
    return handle;
  }

  @Override
  public void restoreSnapshot(ChunkData data, int section) {
    if (section < 0 || section >= data.sections()) {
      throw new IndexOutOfBoundsException(section);
    }
    var levelChunk = handle().getChunkSource().getChunkNow(data.x(), data.z());
    if (levelChunk != null && data instanceof ChunkDataImpl chunkData) {
      var region = data.chunk().region();
      int sectionOffset = (region.min().blockY() >> 4) + section;

      int minY = (sectionOffset << 4) + region.min().blockY() & 15;
      int minZ = region.min().blockZ();
      int minX = region.min().blockX();

      int maxY = (sectionOffset << 4) + region.max().blockY() & 15;
      int maxZ = region.max().blockZ();
      int maxX = region.max().blockX();

      var mutablePos = new BlockPos.MutableBlockPos();
      for (int y = minY; y < maxY; y++) {
        mutablePos.setY(y);
        for (int z = minZ; z < maxZ; z++) {
          mutablePos.setZ(z);
          for (int x = minX; x < maxX; x++) {
            mutablePos.setX(x);
            levelChunk.setBlockState(mutablePos, chunkData.getState(x, y, z), false);
          }
        }
      }
    } else {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public CompletableFuture<ChunkData> snapshot(ChunkRegion chunk) {
    return handle().getChunkSource().getChunkFuture(chunk.x(), chunk.z(), ChunkStatus.EMPTY, false)
      .thenApply(r -> ChunkDataImpl.create(chunk, r.orThrow()));
  }

  @Override
  public CompletableFuture<?> loadChunkWithTicket(int x, int z) {
    var chunkPos = new ChunkPos(x, z);
    return handle().getChunkSource().getChunkFuture(x, z, ChunkStatus.EMPTY, false)
      .thenAccept(c -> handle().getChunkSource().addRegionTicket(GAIA_TICKET_TYPE, chunkPos, 0, chunkPos));
  }

  @Override
  public void removeChunkTicket(int x, int z) {
    var chunkPos = new ChunkPos(x, z);
    handle().getChunkSource().removeRegionTicket(GAIA_TICKET_TYPE, chunkPos, 2, chunkPos);
  }
}
