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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

public interface Section {
  BlockState state(int x, int y, int z);

  void state(int x, int y, int z, int id);

  static Section copy(LevelChunkSection section) {
    if (!section.hasOnlyAir()) {
      return new VanillaSection(section.getStates().copy());
    } else {
      return VanillaSection.empty();
    }
  }

  static Section from(Int2ObjectMap<BlockState> palette, BitStorage storage) {
    return new ChunkSection(palette, storage);
  }
}
