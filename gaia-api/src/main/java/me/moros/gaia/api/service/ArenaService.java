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

package me.moros.gaia.api.service;

import java.util.Optional;
import java.util.stream.Stream;

import me.moros.gaia.api.arena.Arena;
import me.moros.gaia.api.user.GaiaUser;
import me.moros.gaia.api.util.RevertResult;

public interface ArenaService extends Iterable<Arena> {
  boolean contains(String name);

  Optional<Arena> arena(String name);

  void add(Arena arena);

  boolean remove(String name);

  int size();

  Stream<Arena> stream();

  boolean create(GaiaUser user, String arenaName);

  RevertResult revert(Arena arena);

  void cancelRevert(Arena arena);
}
