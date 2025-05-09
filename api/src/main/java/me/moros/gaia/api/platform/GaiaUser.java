/*
 * Copyright 2020-2025 Moros
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

package me.moros.gaia.api.platform;

import java.util.Optional;

import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.arena.Point;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;

public interface GaiaUser extends ForwardingAudience.Single, Locatable {
  Gaia parent();

  default boolean isPlayer() {
    return false;
  }

  default void teleport(Key worldKey, Point point) {
  }

  @Override
  default Optional<Key> level() {
    return Optional.empty();
  }
}
