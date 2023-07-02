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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import me.moros.gaia.api.chunk.ChunkPosition;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.api.region.Region;
import me.moros.math.Vector3i;

public final class ChunkUtil {
  private ChunkUtil() {
  }

  public static List<ChunkPosition> spiralChunks(Region region) {
    final int sizeX = calculateChunkDistance(region.max().blockX(), region.min().blockX());
    final int sizeZ = calculateChunkDistance(region.max().blockZ(), region.min().blockZ());

    final var centerChunk = ChunkPosition.at(region.center());

    final int halfX = sizeX / 2;
    final int halfZ = sizeZ / 2;

    int x = 0, z = 0, dx = 0, dz = -1;
    int t = Math.max(sizeX, sizeZ);
    final int maxI = t * t;
    final List<ChunkPosition> result = new ArrayList<>();
    for (int i = 0; i < maxI; i++) {
      if ((-halfX <= x) && (x <= halfX) && (-halfZ <= z) && (z <= halfZ)) {
        result.add(ChunkPosition.at(centerChunk.x() + x, centerChunk.z() + z));
      }
      if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
        t = dx;
        dx = -dz;
        dz = t;
      }
      x += dx;
      z += dz;
    }
    return result;
  }

  public static Collection<ChunkRegion> splitIntoChunks(Region region) {
    final int minX = region.min().blockX();
    final int maxX = region.max().blockX();

    final int minY = region.min().blockY();
    final int maxY = region.max().blockY();

    final int minZ = region.min().blockZ();
    final int maxZ = region.max().blockZ();

    final int dx = calculateChunkDistance(minX, maxX);
    final int dz = calculateChunkDistance(minZ, maxZ);

    final Collection<ChunkRegion> regions = new ArrayList<>(dx * dz);
    int tempX, tempZ;
    Vector3i v1, v2;
    for (int x = minX >> 4; x <= maxX >> 4; ++x) {
      tempX = x * 16;
      for (int z = minZ >> 4; z <= maxZ >> 4; ++z) {
        tempZ = z * 16;
        v1 = atXZClamped(tempX, minY, tempZ, minX, maxX, minZ, maxZ);
        v2 = atXZClamped(tempX + 15, maxY, tempZ + 15, minX, maxX, minZ, maxZ);
        regions.add(ChunkRegion.create(Region.of(v1, v2)));
      }
    }
    return regions;
  }

  public static boolean isValidRegionSize(Region region) {
    return region.size().blockX() <= 16 && region.size().blockZ() <= 16;
  }

  public static void validateRegionSize(Region region) throws IllegalArgumentException {
    if (!isValidRegionSize(region)) {
      throw new IllegalArgumentException(region.size() + " exceeds chunk size limits!");
    }
  }

  public static int calculateSections(ChunkRegion chunk) {
    int minSectionY = chunk.region().min().blockY() >> 4;
    int maxSectionY = chunk.region().max().blockY() >> 4;
    return 1 + (maxSectionY - minSectionY);
  }

  public static int calculateChunkDistance(int minPos, int maxPos) {
    if (minPos > maxPos) {
      throw new IllegalArgumentException(String.format("Encountered minPos (%d) > maxPos (%d)", minPos, maxPos));
    }
    int cMin = minPos >> 4;
    int cMax = maxPos >> 4;
    return Math.max(1, cMax - cMin);
  }

  private static Vector3i atXZClamped(int x, int y, int z, int minX, int maxX, int minZ, int maxZ) {
    if (minX > maxX || minZ > maxZ) {
      throw new IllegalArgumentException("Minimum cannot be greater than maximum");
    }
    return Vector3i.of(Math.max(minX, Math.min(maxX, x)), y, Math.max(minZ, Math.min(maxZ, z)));
  }
}
