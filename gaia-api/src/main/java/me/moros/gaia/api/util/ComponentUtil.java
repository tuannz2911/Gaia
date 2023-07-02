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

package me.moros.gaia.api.util;

import me.moros.gaia.api.arena.Arena;
import me.moros.math.Vector3i;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class ComponentUtil {
  private ComponentUtil() {
  }

  public static Component generateInfo(Arena arena) {
    int volume = arena.region().volume();
    var size = arena.region().size();
    Vector3i c = arena.region().center();
    final Component infoDetails = Component.text()
      .append(Component.text("Name: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(arena.name(), NamedTextColor.GREEN)).append(Component.newline())
      .append(Component.text("World: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(arena.level().value(), NamedTextColor.GREEN)).append(Component.newline())
      .append(Component.text("Dimensions: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(formatVector(size, " x "), NamedTextColor.GREEN)).append(Component.newline())
      .append(Component.text("Volume: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(volume, NamedTextColor.GREEN)).append(Component.newline())
      .append(Component.text("Center: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(formatVector(c, ", "), NamedTextColor.GREEN))
      .append(Component.newline()).append(Component.newline())
      .append(Component.text("Click to copy center coordinates to clipboard.", NamedTextColor.GRAY))
      .build();
    return Component.text()
      .append(Component.text("> ", NamedTextColor.DARK_GRAY).append(arena.displayName()))
      .append(Component.text(" (" + TextUtil.description(volume) + ")", NamedTextColor.DARK_AQUA))
      .hoverEvent(HoverEvent.showText(infoDetails))
      .clickEvent(ClickEvent.copyToClipboard(formatVector(c, " ")))
      .build();
  }

  private static String formatVector(Vector3i vector, String delimiter) {
    return vector.blockX() + delimiter + vector.blockY() + delimiter + vector.blockZ();
  }
}
