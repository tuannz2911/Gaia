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

package me.moros.gaia.common.storage;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import me.moros.gaia.api.chunk.ChunkData;
import me.moros.math.Vector3i;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinRootEntry;

public class SchemWriter implements Closeable {
  private static final int CURRENT_VERSION = 3;

  private final DataOutputStream outputStream;
  private final int dataVersion;

  public SchemWriter(DataOutputStream outputStream, int dataVersion) {
    this.outputStream = outputStream;
    this.dataVersion = dataVersion;
  }

  public void write(ChunkData data) throws IOException {
    var rootEntry = new LinRootEntry("", LinCompoundTag.builder().put("Schematic", asTag(data)).build());
    LinBinaryIO.write(outputStream, rootEntry);
  }

  //https://github.com/SpongePowered/Schematic-Specification/blob/master/versions/schematic-3.md
  private LinCompoundTag asTag(ChunkData data) {
    int width = data.width();
    int height = data.height();
    int length = data.length();

    LinCompoundTag.Builder schematic = LinCompoundTag.builder();
    schematic.putInt("Version", CURRENT_VERSION);
    schematic.putInt("DataVersion", dataVersion);

    LinCompoundTag.Builder metadata = LinCompoundTag.builder();
    metadata.putLong("Date", System.currentTimeMillis());
    schematic.put("Metadata", metadata.build());

    schematic.putShort("Width", (short) width);
    schematic.putShort("Height", (short) height);
    schematic.putShort("Length", (short) length);
    schematic.putIntArray("Offset", Vector3i.ZERO.toIntArray());
    schematic.put("Blocks", encodeBlocks(data));
    return schematic.build();
  }

  private static final class PaletteMap {
    private final Object2IntMap<String> contents = new Object2IntLinkedOpenHashMap<>();
    private int nextId = 0;

    public int getId(String key) {
      int result = contents.getOrDefault(key, -1);
      if (result != -1) {
        return result;
      }
      int newValue = nextId;
      nextId++;
      contents.put(key, newValue);
      return newValue;
    }

    public LinCompoundTag toNbt() {
      LinCompoundTag.Builder result = LinCompoundTag.builder();
      Object2IntMaps.fastForEach(contents, e -> result.putInt(e.getKey(), e.getIntValue()));
      return result.build();
    }
  }

  private LinCompoundTag encodeBlocks(ChunkData data) {
    int width = data.width();
    int height = data.height();
    int length = data.length();
    PaletteMap paletteMap = new PaletteMap();
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(width * height * length);
    for (int y = 0; y < height; y++) {
      for (int z = 0; z < length; z++) {
        for (int x = 0; x < width; x++) {
          String key = data.getStateString(x, y, z);
          int id = paletteMap.getId(key);
          while ((id & -128) != 0) {
            buffer.write(id & 127 | 128);
            id >>>= 7;
          }
          buffer.write(id);
        }
      }
    }
    return LinCompoundTag.builder().put("Palette", paletteMap.toNbt()).putByteArray("Data", buffer.toByteArray()).build();
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }
}
