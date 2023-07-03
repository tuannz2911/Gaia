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

package me.moros.gaia.common.util;

import java.util.Collection;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.arena.Arena;
import me.moros.gaia.api.locale.Message;
import me.moros.gaia.api.operation.GaiaOperation;
import me.moros.gaia.api.platform.Level;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.api.region.Region;
import me.moros.gaia.api.service.ArenaService;
import me.moros.gaia.api.user.GaiaUser;
import me.moros.gaia.api.util.ChunkUtil;
import me.moros.gaia.api.util.TextUtil;
import net.kyori.adventure.audience.Audience;

public final class UserArenaFactory {
  private final Gaia plugin;
  private final GaiaUser user;
  private final ArenaService arenaService;

  public UserArenaFactory(GaiaUser user) {
    this.plugin = user.parent();
    this.user = user;
    this.arenaService = plugin.coordinator().arenaService();
  }

  public boolean tryCreate(String name) {
    String arenaName = TextUtil.sanitizeInput(name);
    if (arenaName.length() < 3) {
      Message.CREATE_ERROR_VALIDATION.send(user);
      return false;
    }
    if (plugin.coordinator().arenaService().contains(arenaName)) {
      Message.CREATE_ERROR_EXISTS.send(user, arenaName);
      return false;
    }
    return withValidName(arenaName);
  }

  private boolean withValidName(String arenaName) {
    Level level = user.level().map(plugin.coordinator().levelService()::findLevel).orElse(null);
    if (level == null) {
      Message.PLAYER_REQUIRED.send(user);
      return false;
    }
    var region = plugin.coordinator().selectionService().selection(user).orElse(null);
    plugin.coordinator().selectionService().resetSelection(user);
    if (region == null) {
      Message.CREATE_ERROR_SELECTION.send(user);
      return false;
    }
    double radius = region.size().maxComponent();
    if (radius > 1024) { // For safety reasons limit to maximum 64 chunks in any direction
      Message.CREATE_ERROR_SIZE.send(user);
      return false;
    }
    if (region.center().distanceSq(user.position()) > radius * radius) {
      Message.CREATE_ERROR_DISTANCE.send(user);
      return false;
    }
    if (arenaService.stream().filter(a -> a.level().equals(level.key())).map(Arena::region).anyMatch(region::intersects)) {
      Message.CREATE_ERROR_INTERSECTION.send(user);
      return false;
    }
    return withValidSelection(arenaName, level, region);
  }

  private boolean withValidSelection(String arenaName, Level level, Region region) {
    if (!plugin.coordinator().storage().createEmptyArenaFiles(arenaName)) {
      Message.CREATE_ERROR_CRITICAL.send(user);
      return false;
    }
    // TODO warn about chunk aligning
    var chunkRegions = ChunkUtil.splitIntoChunks(region);
    if (chunkRegions.isEmpty()) {
      Message.CREATE_FAIL.send(user, arenaName);
      return false;
    }
    Message.CREATE_ANALYZING.send(user, arenaName);
    createFuture(user, arenaName, level, chunkRegions);
    return true;
  }

  private void createFuture(Audience user, String arenaName, Level level, Collection<ChunkRegion> chunkRegions) {
    var futures = chunkRegions.stream().map(c -> GaiaOperation.snapshotAnalyze(level, c))
      .map(plugin.coordinator().operationService()::add).toList();

    var combined = FutureUtil.createFailFastBatch(futures);
    long startTime = System.currentTimeMillis();
    // TODO Extra validation and counting
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
        plugin.coordinator().arenaService().remove(arenaName);
        if (t instanceof TimeoutException) {
          Message.CREATE_FAIL_TIMEOUT.send(user, arenaName);
        } else {
          Message.CREATE_FAIL.send(user, arenaName);
          plugin.logger().warn(t.getMessage(), t);
        }
        return null;
      });
  }
}
