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

import cloud.commandframework.internal.CommandRegistrationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.SharedSuggestionProvider;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class ForgeCommandRegistrationHandler<C, S extends SharedSuggestionProvider> implements CommandRegistrationHandler {

    private @MonotonicNonNull ForgeCloudCommandManager<C, S> manager;

    protected void initialize(final ForgeCloudCommandManager<C, S> manager) {
        this.manager = manager;
    }

    protected ForgeCloudCommandManager<C, S> manager() {
        return this.manager;
    }

    public static <S>LiteralCommandNode<S> buildRedirect(
            final @NonNull String alias,
            final @NonNull CommandNode<S> destination
    ) {
        final LiteralArgumentBuilder<S> builder = LiteralArgumentBuilder
                .<S>literal(alias)
                .requires(destination.getRequirement())
                .forward(
                        destination.getRedirect(),
                        destination.getRedirectModifier(),
                        destination.isFork()
                )
                .executes(destination.getCommand());

        for(final CommandNode<S> child : destination.getChildren()) {
            builder.then(child);
        }

        return builder.build();
    }
}
