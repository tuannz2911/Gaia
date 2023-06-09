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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.sk89q.worldedit.fabric.FabricAdapter;
import me.moros.gaia.GaiaPlugin;
import me.moros.gaia.SimpleChunkManager;
import me.moros.gaia.api.GaiaChunk;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

final class FabricChunkManager extends SimpleChunkManager {
  private final TicketType<UUID> ticketType;

  FabricChunkManager(GaiaPlugin plugin) {
    super(plugin);
    this.ticketType = TicketType.create("gaia_arena", UUID::compareTo);
  }

  @Override
  public CompletableFuture<?> asyncLoad(GaiaChunk chunk) {
    var cache = ((ServerLevel) FabricAdapter.adapt(chunk.parent().world())).getChunkSource();
    return cache.getChunkFuture(chunk.x(), chunk.z(), ChunkStatus.EMPTY, false)
      .thenAccept(c -> cache.addRegionTicket(ticketType, new ChunkPos(chunk.x(), chunk.z()), 0, chunk.id()));
  }

  @Override
  public void onChunkOperationComplete(GaiaChunk chunk) {
    ((ServerLevel) FabricAdapter.adapt(chunk.parent().world())).getChunkSource()
      .removeRegionTicket(ticketType, new ChunkPos(chunk.x(), chunk.z()), 2, chunk.id());
  }
}
