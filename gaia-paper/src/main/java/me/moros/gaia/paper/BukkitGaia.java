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
import me.moros.gaia.api.service.SelectionService;
import me.moros.gaia.api.service.UserService;
import me.moros.gaia.api.user.GaiaUser;
import me.moros.gaia.common.AbstractGaia;
import me.moros.gaia.common.command.Commander;
import me.moros.gaia.paper.platform.BukkitGaiaUser;
import me.moros.gaia.paper.service.BukkitWorldEditSelectionService;
import me.moros.gaia.paper.service.GaiaSelectionService;
import me.moros.gaia.paper.service.LevelServiceImpl;
import me.moros.gaia.paper.service.UserServiceImpl;
import me.moros.tasker.bukkit.BukkitExecutor;
import me.moros.tasker.executor.SyncExecutor;
import org.bstats.bukkit.Metrics;
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
      .bind(LevelService.class, () -> new LevelServiceImpl(logger()));
    bindSelectionService();
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

  private void bindSelectionService() {
    if (parent.getServer().getPluginManager().isPluginEnabled("WorldEdit")) {
      factory.bind(SelectionService.class, BukkitWorldEditSelectionService::new);
    } else {
      factory.bind(SelectionService.class, () -> new GaiaSelectionService(parent));
    }
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
