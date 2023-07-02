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

package me.moros.gaia.common.platform;

import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.region.ChunkRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

public record ChunkDataImpl(ChunkRegion chunk, Section[] sectionData) implements ChunkData {
  @Override
  public String getStateString(int x, int y, int z) {
    return getState(x, y, z).toString();
  }

  public BlockState getState(int x, int y, int z) {
    return sectionData[y >> 4].state(x, y & 15, z);
  }

  public void setState(int x, int y, int z, int id) {
    sectionData[y >> 4].state(x, y & 15, z, id);
  }

  @Override
  public int sections() {
    return sectionData.length;
  }

  public static ChunkData create(ChunkRegion chunk, ChunkAccess access) {
    LevelChunkSection[] cs = access.getSections();
    int minSectionY = chunk.region().min().blockY() >> 4;
    int maxSectionY = chunk.region().max().blockY() >> 4;
    Section[] sections = new Section[1 + (maxSectionY - minSectionY)];
    for (int i = 0; i < sections.length; i++) {
      sections[i] = Section.copy(cs[minSectionY + i]);
    }
    return new ChunkDataImpl(chunk, sections);
  }
}
