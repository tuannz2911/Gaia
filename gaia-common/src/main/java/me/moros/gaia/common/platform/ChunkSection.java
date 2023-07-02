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

record ChunkSection(Int2ObjectMap<BlockState> palette, BitStorage storage) implements Section {
  @Override
  public BlockState state(int x, int y, int z) {
    return palette.get(storage.get(calculateIndex(x, y, z)));
  }

  @Override
  public void state(int x, int y, int z, int id) {
    storage.set(calculateIndex(x, y, z), id);
  }

  private static int calculateIndex(int x, int y, int z) {
    return (y << 4 | z) << 4 | x;
  }
}
