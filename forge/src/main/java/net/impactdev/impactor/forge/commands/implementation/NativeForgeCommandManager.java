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

import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.SenderMapperHolder;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.meta.SimpleCommandMeta;
import org.incendo.cloud.minecraft.modded.ModdedDefaultCaptionsProvider;
import org.incendo.cloud.minecraft.modded.internal.ModdedParserMappings;
import org.incendo.cloud.minecraft.modded.internal.ModdedPreprocessor;
import org.incendo.cloud.suggestion.SuggestionFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

public abstract class NativeForgeCommandManager<C>
        extends CommandManager<C>
        implements BrigadierManagerHolder<C, CommandSourceStack>, SenderMapperHolder<CommandSourceStack, C>
{
    static final Set<NativeForgeCommandManager<?>> INSTANCES = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private final SenderMapper<CommandSourceStack, C> senderMapper;
    private final CloudBrigadierManager<C, CommandSourceStack> brigadier;
    private final SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory;

    /**
     * Create a new command manager instance.
     *
     * @param executionCoordinator       Execution coordinator instance. When choosing the appropriate coordinator for your
     *                                   project, be sure to consider any limitations noted by the platform documentation.
     * @param commandRegistrationHandler Command registration handler. This will get called every time a new command is
     *                                   registered to the command manager. This may be used to forward command registration
     *                                   to the platform.
     */
    protected NativeForgeCommandManager(
            final @NonNull ExecutionCoordinator<C> executionCoordinator,
            final @NonNull SenderMapper<CommandSourceStack, C> senderMapper,
            final @NonNull CommandRegistrationHandler<C> commandRegistrationHandler,
            final @NonNull Supplier<CommandSourceStack> dummyCommandSourceProvider
    ) {
        super(executionCoordinator, commandRegistrationHandler);

        this.senderMapper = senderMapper;
        this.suggestionFactory = super.suggestionFactory().mapped(TooltipSuggestion::tooltipSuggestion);

        this.brigadier = new CloudBrigadierManager<>(
                this,
                () -> new CommandContext<>(
                        this.senderMapper.map(dummyCommandSourceProvider.get()),
                        this
                ),
                this.senderMapper
        );

        ModdedParserMappings.register(this, this.brigadier);
        this.captionRegistry().registerProvider(new ModdedDefaultCaptionsProvider<>());
        this.registerCommandPreProcessor(new ModdedPreprocessor<>(this.senderMapper));
    }

    @Override
    public final SenderMapper<CommandSourceStack, C> senderMapper() {
        return this.senderMapper;
    }

    @Override
    public final CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }

    @Override
    public final CloudBrigadierManager<C, CommandSourceStack> brigadierManager() {
        return this.brigadier;
    }

    @Override
    public final boolean hasBrigadierManager() {
        return true;
    }

    @Override
    public final @NonNull SuggestionFactory<C, ? extends TooltipSuggestion> suggestionFactory() {
        return this.suggestionFactory;
    }

    final void registrationCalled() {
        this.lockRegistration();
    }
}
