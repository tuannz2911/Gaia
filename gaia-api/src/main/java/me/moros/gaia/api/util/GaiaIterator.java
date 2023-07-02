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

import java.util.Iterator;
import java.util.NoSuchElementException;

import me.moros.math.Position;
import me.moros.math.Vector3i;

public final class GaiaIterator implements Iterator<Vector3i> {
  private final int maxX;
  private final int maxY;
  private final int maxZ;
  private int nextX = 0;
  private int nextY = 0;
  private int nextZ = 0;

  private GaiaIterator(Vector3i max) {
    this(max.blockX(), max.blockY(), max.blockZ());
  }

  private GaiaIterator(int x, int y, int z) {
    this.maxX = x;
    this.maxY = y;
    this.maxZ = z;
  }

  @Override
  public boolean hasNext() {
    return (nextX != Integer.MIN_VALUE);
  }

  @Override
  public Vector3i next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    Vector3i answer = Vector3i.of(nextX, nextY, nextZ);
    if (++nextX >= maxX) {
      nextX = 0;
      if (++nextZ >= maxZ) {
        nextZ = 0;
        if (++nextY >= maxY) {
          nextX = Integer.MIN_VALUE;
        }
      }
    }
    return answer;
  }

  public static Iterator<Vector3i> of(Position dimensions) {
    return new GaiaIterator(Vector3i.ONE.max(dimensions));
  }
}
