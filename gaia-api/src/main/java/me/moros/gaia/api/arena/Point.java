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

package me.moros.gaia.api.arena;

import java.text.NumberFormat;

import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public record Point(double x, double y, double z, float yaw, float pitch) implements Vector3d {
  public Point(Position position, float yaw, float pitch) {
    this(position.x(), position.y(), position.z(), yaw, pitch);
  }

  public Component details() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setMaximumFractionDigits(2);

    return Component.text()
      .append(Component.text("Coordinates: ", NamedTextColor.DARK_AQUA))
      .append(Component.newline())
      .append(Component.text(nf.format(x()) + ", " + nf.format(y()) + ", " + nf.format(z()), NamedTextColor.GREEN))
      .append(Component.newline())
      .append(Component.text("Direction", NamedTextColor.DARK_AQUA))
      .append(Component.newline())
      .append(Component.text(nf.format(yaw) + ", " + nf.format(pitch), NamedTextColor.GREEN))
      .append(Component.newline()).append(Component.newline())
      .append(Component.text("Click to teleport to point.", NamedTextColor.GRAY))
      .build();
  }
}
