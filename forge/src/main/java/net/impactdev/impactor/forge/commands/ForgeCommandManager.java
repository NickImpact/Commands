/*
 * This file is part of ImpactDev Command Manager, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2022 NickImpact
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package net.impactdev.impactor.forge.commands;

import cloud.commandframework.CommandManager;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.logging.PluginLogger;
import net.impactdev.impactor.api.platform.players.PlatformPlayer;
import net.impactdev.impactor.api.platform.plugins.PluginMetadata;
import net.impactdev.impactor.api.platform.sources.PlatformSource;
import net.impactdev.impactor.core.commands.manager.AbstractCommandManager;
import net.impactdev.impactor.core.commands.manager.CommandCoordinator;
import net.impactdev.impactor.core.commands.manager.SourceTranslator;
import net.impactdev.impactor.forge.commands.implementation.server.ForgeServerCommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;

public class ForgeCommandManager extends AbstractCommandManager<CommandSourceStack> {

    private BrigadierMapper mapper;

    public ForgeCommandManager(PluginMetadata metadata, PluginLogger logger) {
        super(metadata, logger);
        this.initialize();
    }

    @Override
    protected CommandManager<CommandSource> create(CommandCoordinator coordinator) {
        ForgeServerCommandManager manager = new ForgeServerCommandManager(
                coordinator,
                this.impactor(),
                this.platform()
        );

        this.mapper = new BrigadierMapper(this.logger, manager.brigadierManager());
        return manager;
    }

    public BrigadierMapper mapper() {
        return this.mapper;
    }

    @Override
    protected SourceTranslator<CommandSourceStack, CommandSource> impactor() {
        return source -> {
            @Nullable Entity entity = source.getEntity();
            if(entity == null) {
                return new ForgeCommandSource(PlatformSource.console(), source);
            }

            if(entity instanceof ServerPlayer) {
                return new ForgeCommandSource(PlatformPlayer.getOrCreate(entity.getUUID()), source);
            }

            return new ForgeCommandSource(PlatformSource.factory().entity(entity.getUUID()), source);
        };

    }

    @Override
    protected SourceTranslator<CommandSource, CommandSourceStack> platform() {
        return source -> ((ForgeCommandSource) source).delegate();
    }

    @Override
    protected void initialize$child() {
        new MinecraftExceptionHandler<CommandSource>()
                .withArgumentParsingHandler()
                .withInvalidSenderHandler()
                .withInvalidSyntaxHandler()
                .withNoPermissionHandler()
                .withCommandExecutionHandler()
                .withDecorator(message -> metadata.name().map(Component::text)
                        .map(prefix -> prefix.color(NamedTextColor.YELLOW)
                                .append(space())
                                .append(text("»")).color(NamedTextColor.GRAY)
                                .append(space())
                                .append(message)
                        )
                        .map(c -> (Component) c)
                        .orElse(message)
                )
                .apply(this.delegate(), source -> source);
    }
}

