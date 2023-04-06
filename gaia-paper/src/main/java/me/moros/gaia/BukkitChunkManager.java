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

import java.util.concurrent.CompletableFuture;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.moros.gaia.api.GaiaChunk;
import org.bukkit.Location;

final class BukkitChunkManager extends SimpleChunkManager {
  private final Gaia bukkitPlugin;

  BukkitChunkManager(Gaia plugin) {
    super(plugin);
    this.bukkitPlugin = plugin;
  }

  @Override
  public CompletableFuture<?> asyncLoad(GaiaChunk chunk) {
    var future = new CompletableFuture<>();
    scheduleWithRegionContext(chunk, () -> BukkitAdapter.adapt(chunk.parent().world()).getChunkAtAsync(chunk.x(), chunk.z())
      .thenAccept(c -> c.addPluginChunkTicket(bukkitPlugin)).thenAccept(future::complete)
    );
    return future;
  }

  @Override
  public void onChunkOperationComplete(GaiaChunk chunk) {
    scheduleWithRegionContext(chunk, () -> BukkitAdapter.adapt(chunk.parent().world()).removePluginChunkTicket(chunk.x(), chunk.z(), bukkitPlugin));
  }

  private void scheduleWithRegionContext(GaiaChunk chunk, Runnable task) {
    var w = BukkitAdapter.adapt(chunk.parent().world());
    var loc = new Location(w, chunk.x() << 4, 0, chunk.z() << 4);
    bukkitPlugin.getServer().getRegionScheduler().execute(bukkitPlugin, loc, task);
  }
}
