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

package me.moros.gaia.common.storage.decoder;

import java.io.IOException;
import java.util.function.Function;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.common.platform.GaiaChunkData;
import me.moros.gaia.common.util.DelegateIterator;
import net.minecraft.SharedConstants;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;

final class DecoderImpl<BlockState> implements Decoder {
  private final int dataVersion;
  private final Function<String, BlockState> mapper;

  DecoderImpl(Function<String, BlockState> mapper) {
    this.dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    this.mapper = mapper;
  }

  @Override
  public int dataVersion() {
    return dataVersion;
  }

  @Override
  public ChunkData decodeBlocks(ChunkRegion chunk, LinCompoundTag paletteObject, byte[] blocks) throws IOException {
    var palette = decodePalette(paletteObject);
    return new GaiaChunkData<>(chunk, palette, blocks, new DelegateIterator<>(blocks, palette::get));
  }

  private Int2ObjectMap<BlockState> decodePalette(LinCompoundTag paletteObject) throws IOException {
    var entrySet = paletteObject.value().entrySet();
    Int2ObjectMap<BlockState> palette = new Int2ObjectOpenHashMap<>(entrySet.size());
    for (var palettePart : entrySet) {
      if (!(palettePart.getValue() instanceof LinIntTag idTag)) {
        throw new IOException("Invalid palette entry: " + palettePart);
      }
      int id = idTag.valueAsInt();
      palette.put(id, mapper.apply(palettePart.getKey()));
    }
    return palette;
  }
}
