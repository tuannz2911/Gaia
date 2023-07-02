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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.region.ChunkRegion;
import me.moros.gaia.api.util.ChunkUtil;
import me.moros.gaia.common.platform.ChunkDataImpl;
import me.moros.gaia.common.platform.Section;
import net.minecraft.SharedConstants;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;

public class DecoderImpl implements Decoder {
  private final Gaia plugin;
  private final int dataVersion;
  private final DataFixer dfu;
  private final Map<String, BlockState> cache;

  public DecoderImpl(Gaia plugin) {
    this.plugin = plugin;
    this.dataVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    this.dfu = DataFixers.getDataFixer();
    this.cache = new ConcurrentHashMap<>();
    rebuildCache();
  }

  private void rebuildCache() {
    cache.clear();
    Block.BLOCK_STATE_REGISTRY.forEach(blockData -> cache.put(blockData.toString(), blockData));
  }

  protected Int2ObjectMap<BlockState> decodePalette(LinCompoundTag paletteObject) throws IOException {
    var entrySet = paletteObject.value().entrySet();
    Int2ObjectMap<BlockState> palette = new Int2ObjectOpenHashMap<>(entrySet.size());
    for (var palettePart : entrySet) {
      if (!(palettePart.getValue() instanceof LinIntTag idTag)) {
        throw new IOException("Invalid palette entry: " + palettePart);
      }
      int id = idTag.valueAsInt();
      // TODO use DFU to update string
      BlockState state = parseStateOrAir(palettePart.getKey());
      palette.put(id, state);
    }
    return palette;
  }

  private BlockState parseStateOrAir(String value) {
    BlockState result = cache.computeIfAbsent(value, this::parseState);
    if (result == null) {
      plugin.logger().warn("Invalid BlockState: " + value + ". Block will be replaced with air.");
      return Blocks.AIR.defaultBlockState();
    } else {
      return result;
    }
  }

  private @Nullable BlockState parseState(String data) {
    try {
      StringReader reader = new StringReader(data);
      var arg = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), reader, false);
      if (reader.canRead()) {
        return arg.blockState();
      }
      return arg.blockState();
    } catch (CommandSyntaxException ignore) {
    }
    return null;
  }

  @Override
  public int dataVersion() {
    return dataVersion;
  }

  private static final int BITS_PER_BLOCK = 15;

  @Override
  public ChunkData decodeBlocks(ChunkRegion chunk, LinCompoundTag paletteObject, byte[] blocks) throws IOException {
    var palette = decodePalette(paletteObject);
    int sectionSize = ChunkUtil.calculateSections(chunk);
    var sections = new Section[sectionSize];
    for (int i = 0; i < sectionSize; i++) {
      sections[i] = Section.from(palette, new SimpleBitStorage(4096, BITS_PER_BLOCK));
    }
    var result = new ChunkDataImpl(chunk, sections);
    int index = 0;
    int i = 0;
    int value = 0;
    int varint_length = 0;
    while (i < blocks.length) {
      value = 0;
      varint_length = 0;
      while (true) {
        value |= (blocks[i] & 127) << (varint_length++ * 7);
        if (varint_length > 5) {
          throw new RuntimeException("VarInt too big (probably corrupted data)");
        }
        if ((blocks[i] & 128) != 128) {
          i++;
          break;
        }
        i++;
      }

      // index = (y * 256) + (z * 16) + x
      final int y = index >> 8;
      final int remainder = index - (y << 8);
      final int z = remainder >> 4;
      final int x = remainder - (z << 4);
      result.setState(x, y, z, value);
      index++;
    }
    return result;
  }
}
