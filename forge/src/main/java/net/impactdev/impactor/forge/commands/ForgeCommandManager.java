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

import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.logging.PluginLogger;
import net.impactdev.impactor.api.platform.plugins.PluginMetadata;
import net.impactdev.impactor.core.commands.manager.AbstractCommandManager;
import net.impactdev.impactor.forge.commands.implementation.ForgeServerCommandManager;
import net.minecraft.commands.CommandSourceStack;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.execution.ExecutionCoordinator;

import java.util.Optional;

public final class ForgeCommandManager extends AbstractCommandManager {

    public ForgeCommandManager(PluginMetadata metadata, PluginLogger logger) {
        super(metadata, logger);
        this.initialize();
    }

    @Override
    protected CommandManager<CommandSource> create(ExecutionCoordinator<CommandSource> coordinator) {
        final SenderMapper<CommandSourceStack, CommandSource> mapper = new ForgeSenderMapper();
        return new ForgeServerCommandManager<>(coordinator, mapper);
    }

}

