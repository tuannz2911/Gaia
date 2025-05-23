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

package me.moros.gaia.common.command.parser;

import me.moros.gaia.api.arena.Arena;
import me.moros.gaia.api.platform.GaiaUser;
import me.moros.gaia.api.util.TextUtil;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class ArenaParser implements ArgumentParser<GaiaUser, Arena>, BlockingSuggestionProvider.Strings<GaiaUser> {
  private ArenaParser() {
  }

  @Override
  public ArgumentParseResult<Arena> parse(CommandContext<GaiaUser> commandContext, CommandInput commandInput) {
    String input = TextUtil.sanitizeInput(commandInput.peekString());
    Arena result;
    GaiaUser user = commandContext.sender();
    if (input.equalsIgnoreCase("cur")) {
      var pos = user.position().toVector3i();
      result = user.level().flatMap(levelKey -> user.parent().arenaService().arena(levelKey, pos)).orElse(null);
    } else {
      result = user.parent().arenaService().arena(input).orElse(null);
    }
    if (result != null) {
      commandInput.readString();
      return ArgumentParseResult.success(result);
    } else {
      return ArgumentParseResult.failure(new Throwable("Could not find the specified arena"));
    }
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<GaiaUser> commandContext, CommandInput commandInput) {
    return commandContext.sender().parent().arenaService().stream().map(Arena::name).sorted().toList();
  }

  public static ParserDescriptor<GaiaUser, Arena> parser() {
    return ParserDescriptor.of(new ArenaParser(), Arena.class);
  }
}


