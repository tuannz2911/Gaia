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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.platform.Level;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.common.util.DelegateIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;

@SuppressWarnings("resource")
public abstract class VanillaLevel implements Level {
  private static final TicketType<Unit> GAIA_TICKET_TYPE = TicketType.create("gaia", (u1, u2) -> 0);

  private final ServerLevel handle;

  protected VanillaLevel(ServerLevel handle) {
    this.handle = handle;
  }

  protected ServerLevel handle() {
    return handle;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean restoreSnapshot(ChunkData data, int amount) {
    var chunkSource = chunkSource();
    var levelChunk = chunkSource.getChunkNow(data.x(), data.z());
    if (levelChunk != null && amount > 0 && data instanceof GaiaChunkData<?> chunkData) {
      var offset = chunkData.chunk().region().min();
      final int xOffset = offset.blockX();
      final int yOffset = offset.blockY();
      final int zOffset = offset.blockZ();
      final DelegateIterator<BlockState> it = (DelegateIterator<BlockState>) chunkData.cachedIterator();
      final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
      int counter = 0;
      int index;
      BlockState toRestore;
      while (it.hasNext() && ++counter <= amount) {
        index = it.index();
        final int y = index >> 8;
        final int remainder = index - (y << 8);
        final int z = remainder >> 4;
        final int x = remainder - (z << 4);
        toRestore = it.next();
        BlockState result = levelChunk.setBlockState(mutablePos.set(x, yOffset + y, z), toRestore, false);
        if (result != null && result != toRestore) {
          chunkSource.blockChanged(mutablePos.move(xOffset, 0, zOffset));
        }
      }
      return it.hasNext();
    }
    return false;
  }

  @Override
  public ChunkData snapshot(ChunkRegion chunk) {
    var levelChunk = Objects.requireNonNull(chunkSource().getChunkNow(chunk.x(), chunk.z()), "Chunk not loaded!");
    return VanillaChunkData.from(chunk, levelChunk);
  }

  @Override
  public CompletableFuture<?> loadChunkWithTicket(int x, int z) {
    var chunkPos = new ChunkPos(x, z);
    return chunkSource().getChunkFuture(x, z, ChunkStatus.EMPTY, false)
      .thenAccept(c -> chunkSource().addRegionTicket(GAIA_TICKET_TYPE, chunkPos, 0, Unit.INSTANCE));
  }

  @Override
  public void removeChunkTicket(int x, int z) {
    var chunkPos = new ChunkPos(x, z);
    chunkSource().removeRegionTicket(GAIA_TICKET_TYPE, chunkPos, 2, Unit.INSTANCE);
  }

  private ServerChunkCache chunkSource() {
    return handle().getChunkSource();
  }
}
