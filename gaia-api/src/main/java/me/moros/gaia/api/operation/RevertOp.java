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

package me.moros.gaia.api.operation;

import me.moros.gaia.api.chunk.ChunkData;
import me.moros.gaia.api.platform.Level;

final class RevertOp extends AbstractOp.LevelChunkOp<Void> implements GaiaOperation.Revert {
  private final ChunkData data;
  private int currentSection;

  RevertOp(Level level, ChunkData data) {
    super(level, data.chunk());
    this.data = data;
    this.currentSection = 0;
  }

  @Override
  protected Result processStep() {
    level.restoreSnapshot(data, currentSection);
    if (++currentSection < data.sections()) {
      return Result.CONTINUE;
    } else {
      future.complete(null);
      return Result.REMOVE;
    }
  }
}
