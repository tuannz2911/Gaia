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

package me.moros.gaia.api.user;

import java.util.Optional;
import java.util.function.Predicate;

import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.arena.Arena;
import me.moros.gaia.api.arena.Point;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface GaiaUser extends ForwardingAudience.Single, Locatable {
  Gaia parent();

  default boolean isPlayer() {
    return false;
  }

  default void teleport(Key worldKey, Point point) {
  }

  default @Nullable Arena standingArena() {
    Key worldId = worldKey().orElse(null);
    if (worldId == null) {
      return null;
    }
    Vector3d point = position();
    Predicate<Arena> matcher = a -> a.level().equals(worldId) && a.region().contains(point);
    return parent().coordinator().arenaManager().stream().filter(matcher).findAny().orElse(null);
  }

  default Optional<Point> createPoint() {
    if (isPlayer()) {
      return Optional.of(new Point(position(), yaw(), pitch()));
    }
    return Optional.empty();
  }
}
