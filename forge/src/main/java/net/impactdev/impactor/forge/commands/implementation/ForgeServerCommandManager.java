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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.modded.internal.ModdedParserMappings;

import java.util.concurrent.ExecutionException;

public class ForgeServerCommandManager<C> extends NativeForgeCommandManager<C> {

    public static ForgeServerCommandManager<CommandSourceStack> createNative(final ExecutionCoordinator<CommandSourceStack> coordinator) {
        return new ForgeServerCommandManager<>(coordinator, SenderMapper.identity());
    }

    private final Cache<String, PermissionNode<Boolean>> permissionNodeCache = CacheBuilder.newBuilder().maximumSize(100).build();

    /**
     * Create a new command manager instance.
     *
     * @param executionCoordinator       Execution coordinator instance. When choosing the appropriate coordinator for your
     *                                   project, be sure to consider any limitations noted by the platform documentation.
     * @param senderMapper
     */
    public ForgeServerCommandManager(@NonNull ExecutionCoordinator<C> executionCoordinator, @NonNull SenderMapper<CommandSourceStack, C> senderMapper) {
        super(
            executionCoordinator,
            senderMapper,
            new ForgeCommandRegistrationHandler.Server<>(),
            () -> new CommandSourceStack(
                    CommandSource.NULL,
                    Vec3.ZERO,
                    Vec2.ZERO,
                    null,
                    4,
                    "",
                    Component.empty(),
                    null,
                    null
            )
        );

        ModdedParserMappings.registerServer(this);
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @Override
    public boolean hasPermission(@NonNull C sender, @NonNull String permission) {
        final CommandSourceStack source = this.senderMapper().reverse(sender);
        if(source.isPlayer()) {
            final PermissionNode<Boolean> node;
            try {
                node = this.permissionNodeCache.get(permission, () -> (PermissionNode<Boolean>) PermissionAPI.getRegisteredNodes().stream()
                        .filter(n -> n.getNodeName().equals(permission) && n.getType() == PermissionTypes.BOOLEAN)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Could not find registered node for permission: " + permission))
                );
            } catch (final ExecutionException exception) {
                throw new RuntimeException("Exception locating permission node", exception);
            }

            return PermissionAPI.getPermission(source.getPlayer(), node);
        }

        return source.hasPermission(source.getServer().getOperatorUserPermissionLevel());
    }
}
