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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public final class BatchProcessor<T> {
  private final int batchSize;
  private final BlockingQueue<T> queue;

  public BatchProcessor(int batchSize) {
    this.batchSize = batchSize;
    this.queue = new LinkedBlockingQueue<>();
  }

  public boolean enqueue(T element) {
    return queue.add(element);
  }

  public List<List<T>> flush() {
    List<T> sink = new ArrayList<>(queue.size());
    queue.drainTo(sink);
    return ListUtil.partition(sink, batchSize);
  }

  public List<T> createBatch() {
    return queue.isEmpty() ? List.of() : batch();
  }

  private List<T> batch() {
    List<T> sink = new ArrayList<>(batchSize);
    queue.drainTo(sink, batchSize);
    return sink;
  }
}
