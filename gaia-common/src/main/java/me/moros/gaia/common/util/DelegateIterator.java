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

package me.moros.gaia.common.util;

import java.util.function.IntFunction;

public final class DelegateIterator<T> implements DataIterator<T> {
  private final byte[] data;
  private final IntFunction<T> mapper;
  private VarIntIterator iterator;

  public DelegateIterator(byte[] data, IntFunction<T> mapper) {
    this.data = data;
    this.mapper = mapper;
    reset();
  }

  @Override
  public int index() {
    return iterator.index();
  }

  @Override
  public void reset() {
    this.iterator = new VarIntIterator(data);
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public T next() {
    return mapper.apply(iterator.next());
  }
}
