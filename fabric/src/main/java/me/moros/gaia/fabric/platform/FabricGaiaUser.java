/*
 * Copyright 2020-2025 Moros
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

package me.moros.gaia.fabric.platform;

import java.util.Optional;
import java.util.Set;

import me.moros.gaia.api.Gaia;
import me.moros.gaia.api.arena.Point;
import me.moros.gaia.api.platform.GaiaUser;
import me.moros.gaia.common.platform.AbstractUser;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FabricGaiaUser extends AbstractUser<CommandSourceStack> {
  private FabricGaiaUser(Gaia parent, CommandSourceStack handle) {
    super(parent, handle);
  }

  @Override
  public Audience audience() {
    return handle();
  }

  public static final class FabricGaiaPlayer extends FabricGaiaUser {
    private final ServerPlayer player;

    private FabricGaiaPlayer(Gaia plugin, CommandSourceStack stack, ServerPlayer player) {
      super(plugin, stack);
      this.player = player;
    }

    @Override
    public boolean isPlayer() {
      return true;
    }

    @Override
    public void teleport(Key worldKey, Point point) {
      for (ServerLevel level : player.getServer().getAllLevels()) {
        if (worldKey.equals(level.dimension().location())) {
          player.teleportTo(level, point.x(), point.y(), point.z(), Set.of(), point.yaw(), point.pitch(), true);
          return;
        }
      }
    }

    @Override
    public Optional<Key> level() {
      return Optional.of(player.level().dimension().location());
    }

    @Override
    public Vector3d position() {
      var pos = player.position();
      return Vector3d.of(pos.x(), pos.y(), pos.z());
    }

    @Override
    public float yaw() {
      return player.getYRot();
    }

    @Override
    public float pitch() {
      return player.getXRot();
    }
  }

  public static GaiaUser from(Gaia parent, CommandSourceStack stack) {
    ServerPlayer player = stack.getPlayer();
    return player != null ? new FabricGaiaPlayer(parent, stack, player) : new FabricGaiaUser(parent, stack);
  }
}
