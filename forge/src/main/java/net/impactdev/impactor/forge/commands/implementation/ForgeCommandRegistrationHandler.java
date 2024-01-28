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

package net.impactdev.impactor.forge.commands.implementation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.impactdev.impactor.forge.commands.implementation.mixins.CommandSelectionAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.brigadier.CloudBrigadierCommand;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.minecraft.modded.ModdedCommandMetaKeys;
import org.incendo.cloud.minecraft.modded.internal.ContextualArgumentTypeProvider;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.incendo.cloud.brigadier.util.BrigadierUtil.buildRedirect;

abstract class ForgeCommandRegistrationHandler<C> implements CommandRegistrationHandler<C> {

    private @MonotonicNonNull NativeForgeCommandManager<C> manager;

    void initialize(final NativeForgeCommandManager<C> manager) {
        this.manager = manager;
    }

    NativeForgeCommandManager<C> manager() {
        return this.manager;
    }

    protected final void registerCommand(@NonNull Command<C> command, final CommandDispatcher<CommandSourceStack> dispatcher) {
        final RootCommandNode<CommandSourceStack> rootNode = dispatcher.getRoot();
        final CommandComponent<C> first = command.rootComponent();
        final CommandNode<CommandSourceStack> baseNode = this.manager()
                .brigadierManager()
                .literalBrigadierNodeFactory()
                .createNode(
                        first.name(),
                        command,
                        new CloudBrigadierCommand<>(this.manager(), this.manager().brigadierManager())
                );

        rootNode.addChild(baseNode);

        for (final String alias : first.alternativeAliases()) {
            rootNode.addChild(buildRedirect(alias, baseNode));
        }
    }

    static class Client<C> extends ForgeCommandRegistrationHandler<C> {

        private final Set<Command<C>> registered = ConcurrentHashMap.newKeySet();
        private volatile boolean registerEventFired = false;

        @Override
        void initialize(NativeForgeCommandManager<C> manager) {
            super.initialize(manager);

            MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
            MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> this.registerEventFired = false);
        }

        public void registerCommands(final RegisterClientCommandsEvent event) {
            this.registerEventFired = true;
            ContextualArgumentTypeProvider.withBuildContext(
                    this.manager(),
                    event.getBuildContext(),
                    true,
                    () -> {
                        for(final Command<C> command : this.registered) {
                            this.registerCommand(command, event.getDispatcher());
                        }
                    }
            );
        }

        @Override
        public boolean registerCommand(@NonNull Command<C> command) {
            this.registered.add(command);
            if(this.registerEventFired) {
                final ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if(connection == null) {
                    throw new IllegalStateException("Expected connection to be present, but it wasn't...");
                }

                final CommandDispatcher<CommandSourceStack> dispatcher = ClientCommandHandler.getDispatcher();
                if(dispatcher == null) {
                    throw new IllegalStateException("Expected an active dispatcher...");
                }

                ContextualArgumentTypeProvider.withBuildContext(
                        this.manager(),
                        CommandBuildContext.simple(connection.registryAccess(), connection.enabledFeatures()),
                        false,
                        () -> this.registerCommand(command, dispatcher)
                );
            }

            return true;
        }
    }

    static class Server<C> extends ForgeCommandRegistrationHandler<C> {

        private final Set<Command<C>> registeredCommands = ConcurrentHashMap.newKeySet();

        @Override
        void initialize(final NativeForgeCommandManager<C> manager) {
            super.initialize(manager);
            MinecraftForge.EVENT_BUS.addListener(this::registerAllCommands);
        }

        @Override
        public boolean registerCommand(final @NonNull Command<C> command) {
            return this.registeredCommands.add(command);
        }

        private void registerAllCommands(final RegisterCommandsEvent event) {
            this.manager().registrationCalled();
            ContextualArgumentTypeProvider.withBuildContext(
                    this.manager(),
                    event.getBuildContext(),
                    true,
                    () -> {
                        for (final Command<C> command : this.registeredCommands) {
                            /* Only register commands in the declared environment */
                            final Commands.CommandSelection env = command.commandMeta().getOrDefault(
                                    ModdedCommandMetaKeys.REGISTRATION_ENVIRONMENT,
                                    Commands.CommandSelection.ALL
                            );

                            CommandSelectionAccessor accessor = (CommandSelectionAccessor) (Object) event.getCommandSelection();
                            if ((env == Commands.CommandSelection.INTEGRATED && !accessor.integrated())
                                    || (env == Commands.CommandSelection.DEDICATED && !accessor.dedicated())) {
                                continue;
                            }
                            this.registerCommand(command, event.getDispatcher());
                        }
                    }
            );
        }

    }
}
