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

package me.moros.gaia.api.util;

import me.moros.math.Position;

public final class CoordinateUtil {
  private CoordinateUtil() {
  }

  private static final int WORLD_XZ_MINMAX = 30_000_000;
  private static final int WORLD_Y_MIN = -2048;
  private static final int WORLD_Y_MAX = 2047;

  private static boolean isValidXZ(int x, int z) {
    return -WORLD_XZ_MINMAX <= x && x <= WORLD_XZ_MINMAX
      && -WORLD_XZ_MINMAX <= z && z <= WORLD_XZ_MINMAX;
  }

  private static boolean isValidY(int n) {
    return WORLD_Y_MIN <= n && n <= WORLD_Y_MAX;
  }

  public static boolean isValidPosition(Position pos) {
    return isValidXZ(pos.blockX(), pos.blockZ()) && isValidY(pos.blockY());
  }

  public static void ensureValidPosition(Position pos) throws IllegalArgumentException {
    if (!isValidPosition(pos)) {
      throw new IllegalArgumentException(String.format("Position %s exceeds bounds!", pos));
    }
  }
}
