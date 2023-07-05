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

package me.moros.gaia.api.arena;

import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import me.moros.gaia.api.locale.Message;
import net.kyori.adventure.text.Component;

public sealed interface RevertResult permits RevertResultImpl {
  Component message();

  CompletableFuture<OptionalLong> future();

  static RevertResult success(Arena arena, CompletableFuture<OptionalLong> future) {
    return new RevertResultImpl(() -> Message.REVERT_SUCCESS.build(arena.displayName()), future);
  }

  static RevertResult reverting(Arena arena) {
    return new RevertResultImpl(() -> Message.REVERT_ERROR_REVERTING.build(arena.displayName()));
  }

  static RevertResult unloaded(Arena arena) {
    return new RevertResultImpl(() -> Message.REVERT_ERROR_UNLOADED.build(arena.displayName()));
  }
}
