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

package me.moros.gaia.paper;

import java.nio.file.Path;

import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.gaia.api.service.LevelService;
import me.moros.gaia.api.service.UserService;
import me.moros.gaia.api.user.GaiaUser;
import me.moros.gaia.common.AbstractGaia;
import me.moros.gaia.common.command.Commander;
import me.moros.gaia.common.platform.LevelAdapter;
import me.moros.gaia.paper.platform.BukkitGaiaUser;
import me.moros.gaia.paper.platform.BukkitLevel;
import me.moros.gaia.paper.service.LevelServiceImpl;
import me.moros.gaia.paper.service.UserServiceImpl;
import me.moros.tasker.bukkit.BukkitExecutor;
import me.moros.tasker.executor.SyncExecutor;
import org.bstats.bukkit.Metrics;
import org.bukkit.World;
import org.slf4j.Logger;

public class BukkitGaia extends AbstractGaia<GaiaBootstrap> {
  private Commander commander;

  protected BukkitGaia(GaiaBootstrap parent, Path path, Logger logger) {
    super(parent, path, logger);
  }

  void onPluginEnable() {
    new Metrics(parent, 8608);
    factory
      .bind(SyncExecutor.class, () -> new BukkitExecutor(parent))
      .bind(UserService.class, () -> new UserServiceImpl(this, parent.getServer()))
      .bind(LevelService.class, () -> new LevelServiceImpl(logger(), loadNativeAdapter()));
    load();

    try {
      PaperCommandManager<GaiaUser> manager = new PaperCommandManager<>(parent,
        CommandExecutionCoordinator.simpleCoordinator(),
        c -> BukkitGaiaUser.from(this, c),
        u -> ((BukkitGaiaUser) u).handle()
      );
      manager.registerAsynchronousCompletions();
      commander = Commander.create(manager, this);
    } catch (Exception e) {
      logger().error(e.getMessage(), e);
      parent.getPluginLoader().disablePlugin(parent);
    }
  }

  void onPluginDisable() {
    disable();
  }

  @SuppressWarnings("unchecked")
  private LevelAdapter<World> loadNativeAdapter() {
    String fullName = parent.getServer().getClass().getPackageName();
    String nmsVersion = fullName.substring(1 + fullName.lastIndexOf("."));
    String className = "me.moros.gaia.paper.nms." + nmsVersion + ".LevelAdapterImpl";
    try {
      Class<?> cls = Class.forName(className);
      if (!cls.isSynthetic() && LevelAdapter.class.isAssignableFrom(cls)) {
        return (LevelAdapter<World>) cls.getDeclaredConstructor().newInstance();
      }
    } catch (Exception ignore) {
      String s = String.format("""

        ****************************************************************
        * Unable to find native module for version %s.
        * Chunk relighting will not be available during this session.
        ****************************************************************

        """, nmsVersion);
      logger().warn(s);
    }
    return w -> new BukkitLevel(w, parent);
  }

  @Override
  public String author() {
    return parent.getDescription().getAuthors().get(0);
  }

  @Override
  public String version() {
    return parent.getDescription().getVersion();
  }
}
