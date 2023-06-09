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

package me.moros.gaia.fabric;

import com.sk89q.worldedit.math.Vector3;
import me.moros.gaia.GaiaPlugin;
import me.moros.gaia.api.GaiaUser;
import me.moros.gaia.fabric.mixin.accessor.CommandSourceStackAccess;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FabricGaiaUser implements GaiaUser {
  private final CommandSourceStack stack;
  private final GaiaPlugin parent;

  private FabricGaiaUser(GaiaPlugin parent, CommandSourceStack source) {
    this.parent = parent;
    this.stack = source;
  }

  public CommandSourceStack stack() {
    return this.stack;
  }

  @Override
  public GaiaPlugin parent() {
    return parent;
  }

  @Override
  public @Nullable Key worldKey() {
    return null;
  }

  @Override
  public boolean isPlayer() {
    return false;
  }

  @Override
  public @Nullable Vector3 position() {
    return null;
  }

  @Override
  public @NonNull Audience audience() {
    return stack;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof FabricGaiaUser other) {
      return ((CommandSourceStackAccess) this.stack).source().equals(((CommandSourceStackAccess) other.stack).source());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ((CommandSourceStackAccess) this.stack).source().hashCode();
  }

  public static final class FabricGaiaPlayer extends FabricGaiaUser {
    private final ServerPlayer player;

    private FabricGaiaPlayer(GaiaPlugin plugin, CommandSourceStack stack) {
      super(plugin, stack);
      this.player = ((ServerPlayer) ((CommandSourceStackAccess) stack()).source());
    }

    @Override
    public Key worldKey() {
      return player.getLevel().dimension().location();
    }

    @Override
    public boolean isPlayer() {
      return true;
    }

    @Override
    public Vector3 position() {
      var pos = player.position();
      return Vector3.at(pos.x(), pos.y(), pos.z());
    }
  }

  public static GaiaUser from(GaiaPlugin parent, CommandSourceStack stack) {
    if (((CommandSourceStackAccess) stack).source() instanceof ServerPlayer) {
      return new FabricGaiaPlayer(parent, stack);
    }
    return new FabricGaiaUser(parent, stack);
  }
}
