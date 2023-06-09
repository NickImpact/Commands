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

import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.standard.UUIDArgument;
import cloud.commandframework.brigadier.BrigadierManagerHolder;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import cloud.commandframework.brigadier.argument.WrappedBrigadierParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.internal.CommandRegistrationHandler;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.meta.SimpleCommandMeta;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeToken;
import net.impactdev.impactor.forge.commands.implementation.arguments.RegistryEntryArgument;
import net.impactdev.impactor.forge.commands.implementation.arguments.TeamArgument;
import net.impactdev.impactor.forge.commands.implementation.arguments.parsers.ForgeArgumentParsers;
import net.impactdev.impactor.forge.commands.implementation.captions.ForgeCaptionRegistry;
import net.impactdev.impactor.forge.commands.implementation.data.MinecraftTime;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.AngleArgument;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.commands.arguments.ObjectiveCriteriaArgument;
import net.minecraft.commands.arguments.OperationArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.SwizzleArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ForgeCloudCommandManager<C, S extends SharedSuggestionProvider>
        extends CommandManager<C> implements BrigadierManagerHolder<C> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MOD_PUBLIC_STATIC_FINAL = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

    private final Function<S, C> sourceMapper;
    private final Function<C, S> backwardSourceMapper;
    private final CloudBrigadierManager<C, S> brigadier;

    /**
     * Create a new command manager instance
     *
     * @param executor              Execution coordinator instance. The coordinator is in charge of executing incoming
     *                              commands. Some considerations must be made when picking a suitable execution coordinator
     *                              for your platform. For example, an entirely asynchronous coordinator is not suitable
     *                              when the parsers used in that particular platform are not thread safe. If you have
     *                              commands that perform blocking operations, however, it might not be a good idea to
     *                              use a synchronous execution coordinator. In most cases you will want to pick between
     *                              {@link CommandExecutionCoordinator#simpleCoordinator()} and
     *                              {@link AsynchronousCommandExecutionCoordinator}
     * @param registrar             Command registration handler. This will get called every time a new command is
     *                              registered to the command manager. This may be used to forward command registration
     *                              to the platform.
     * @param sourceMapper          Maps a {@link SharedSuggestionProvider} to the target command sender type
     * @param backwardsSourceMapper Maps the target command sender type to a {@link SharedSuggestionProvider}
     * @param dummySourceProvider   Provides a dummy command source, for use with brigadier registration
     */
    protected ForgeCloudCommandManager(
            final @NonNull Function<@NonNull CommandTree<C>, @NonNull CommandExecutionCoordinator<C>> executor,
            final @NonNull CommandRegistrationHandler registrar,
            final @NonNull Function<S, C> sourceMapper,
            final @NonNull Function<C, S> backwardsSourceMapper,
            final @NonNull Supplier<S> dummySourceProvider
    ) {
        super(executor, registrar);
        this.sourceMapper = sourceMapper;
        this.backwardSourceMapper = backwardsSourceMapper;
        this.brigadier = new CloudBrigadierManager<>(this, () -> new CommandContext<>(
                this.sourceMapper.apply(dummySourceProvider.get()),
                this
        ));

        this.brigadier.brigadierSenderMapper(this.sourceMapper);
        this.brigadier.backwardsBrigadierSenderMapper(this.backwardSourceMapper);
        this.registerNativeBrigadierMappings(this.brigadier);
        this.setCaptionRegistry(new ForgeCaptionRegistry<>());
        this.registerCommandPreProcessor(new ForgeCommandPreprocessor<>(this));

        ((ForgeCommandRegistrationHandler<C, S>) this.getCommandRegistrationHandler()).initialize(this);

    }

    private void registerNativeBrigadierMappings(final @NonNull CloudBrigadierManager<C, S> brigadier) {
        /* Cloud-native argument types */
        brigadier.registerMapping(new TypeToken<UUIDArgument.UUIDParser<C>>(){}, builder -> builder.toConstant(UuidArgument.uuid()));

        this.registerRegistryEntryMappings();
        brigadier.registerMapping(
                new TypeToken<TeamArgument.TeamParser<C>>(){},
                builder -> builder.toConstant(net.minecraft.commands.arguments.TeamArgument.team())
        );

        this.getParserRegistry().registerParserSupplier(
                TypeToken.get(PlayerTeam.class),
                params -> new TeamArgument.TeamParser<>()
        );

        // Wrapped/Constant Brigadier types, native value type
        this.registerConstantNativeParserSupplier(ChatFormatting.class, ColorArgument.color());
        this.registerConstantNativeParserSupplier(CompoundTag.class, CompoundTagArgument.compoundTag());
        this.registerConstantNativeParserSupplier(Tag.class, NbtTagArgument.nbtTag());
        this.registerConstantNativeParserSupplier(NbtPathArgument.NbtPath.class, NbtPathArgument.nbtPath());
        this.registerConstantNativeParserSupplier(ObjectiveCriteria.class, ObjectiveCriteriaArgument.criteria());
        this.registerConstantNativeParserSupplier(OperationArgument.Operation.class, OperationArgument.operation());
        this.registerConstantNativeParserSupplier(ParticleOptions.class, ParticleArgument.particle());
        this.registerConstantNativeParserSupplier(AngleArgument.SingleAngle.class, AngleArgument.angle());
        this.registerConstantNativeParserSupplier(new TypeToken<>() {}, SwizzleArgument.swizzle());
        this.registerConstantNativeParserSupplier(ResourceLocation.class, ResourceLocationArgument.id());
        this.registerConstantNativeParserSupplier(EntityAnchorArgument.Anchor.class, EntityAnchorArgument.anchor());
        this.registerConstantNativeParserSupplier(MinMaxBounds.Ints.class, RangeArgument.intRange());
        this.registerConstantNativeParserSupplier(MinMaxBounds.Doubles.class, RangeArgument.floatRange());
        this.registerContextualNativeParserSupplier(ItemInput.class, ItemArgument::item);
        this.registerContextualNativeParserSupplier(BlockPredicateArgument.Result.class, BlockPredicateArgument::blockPredicate);

        // Wrapped/Constant Brigadier types, mapped value type
        this.registerConstantNativeParserSupplier(MessageArgument.Message.class, MessageArgument.message());
        this.getParserRegistry().registerParserSupplier(
                TypeToken.get(MinecraftTime.class),
                params -> ForgeArgumentParsers.time()
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerRegistryEntryMappings() {
        this.brigadier.registerMapping(
                new TypeToken<RegistryEntryArgument.Parser<C, ?>>() {
                },
                builder -> {
                    builder.to(argument -> ResourceKeyArgument.key((ResourceKey) argument.registryKey()));
                }
        );

        /* Find all fields of RegistryKey<? extends Registry<?>> and register those */
        /* This only works for vanilla registries really, we'll have to do other things for non-vanilla ones */
        final Set<Class<?>> seenClasses = new HashSet<>();
        /* Some registries have types that are too generic... we'll skip those for now.
         * Eventually, these could be resolved by using ParserParameters in some way? */
        seenClasses.add(ResourceLocation.class);
        seenClasses.add(Codec.class);
        for (final Field field : Registry.class.getDeclaredFields()) {
            if ((field.getModifiers() & MOD_PUBLIC_STATIC_FINAL) != MOD_PUBLIC_STATIC_FINAL) {
                continue;
            }
            if (!field.getType().equals(ResourceKey.class)) {
                continue;
            }

            final Type generic = field.getGenericType(); /* RegistryKey<? extends Registry<?>> */
            if (!(generic instanceof ParameterizedType)) {
                continue;
            }

            Type registryType = ((ParameterizedType) generic).getActualTypeArguments()[0];
            while (registryType instanceof WildcardType) {
                registryType = ((WildcardType) registryType).getUpperBounds()[0];
            }

            if (!(registryType instanceof ParameterizedType)) { /* expected: Registry<V> */
                continue;
            }

            final ResourceKey<?> key;
            try {
                key = (ResourceKey<?>) field.get(null);
            } catch (final IllegalAccessException ex) {
                LOGGER.warn("Failed to access value of registry key in field {} of type {}", field.getName(), generic, ex);
                continue;
            }

            final Type valueType = ((ParameterizedType) registryType).getActualTypeArguments()[0];
            if (seenClasses.contains(GenericTypeReflector.erase(valueType))) {
                LOGGER.debug("Encountered duplicate type in registry {}: type {}", key, valueType);
                continue;
            }
            seenClasses.add(GenericTypeReflector.erase(valueType));

            /* and now, finally, we can register */
            this.getParserRegistry().registerParserSupplier(
                    TypeToken.get(valueType),
                    params -> new RegistryEntryArgument.Parser(key)
            );
        }
    }

    /**
     * Register a parser supplier for a brigadier type that has no options and whose output can be directly used.
     *
     * @param type     the Java type to map
     * @param argument the Brigadier parser
     * @param <T>      value type
     * @since 1.5.0
     */
    final <T> void registerConstantNativeParserSupplier(final @NonNull Class<T> type, final @NonNull ArgumentType<T> argument) {
        this.registerConstantNativeParserSupplier(TypeToken.get(type), argument);
    }

    /**
     * Register a parser supplier for a brigadier type that has no options and whose output can be directly used.
     *
     * @param type     the Java type to map
     * @param argument the Brigadier parser
     * @param <T>      value type
     * @since 1.5.0
     */
    final <T> void registerConstantNativeParserSupplier(
            final @NonNull TypeToken<T> type,
            final @NonNull ArgumentType<T> argument
    ) {
        this.getParserRegistry().registerParserSupplier(type, params -> new WrappedBrigadierParser<>(argument));
    }

    /**
     * Register a parser supplier for a brigadier type that has no options and whose output can be directly used.
     *
     * @param type     the Java type to map
     * @param argument a function providing the Brigadier parser given a build context
     * @param <T>      value type
     * @since 1.7.0
     */
    final <T> void registerContextualNativeParserSupplier(
            final @NonNull Class<T> type,
            final @NonNull Function<CommandBuildContext, @NonNull ArgumentType<T>> argument
    ) {
        this.parserRegistry().registerParserSupplier(
                TypeToken.get(type),
                params -> ForgeArgumentParsers.contextual(argument)
        );
    }

    public Function<S, C> sourceMapper() {
        return this.sourceMapper;
    }

    public Function<C, S> backwardsSourceMapper() {
        return this.backwardSourceMapper;
    }

    public final void registrationCalled() {
        this.lockRegistration();
    }

    @Override
    public final @NonNull CloudBrigadierManager<C, S> brigadierManager() {
        return this.brigadier;
    }

    @Override
    public final @NonNull CommandMeta createDefaultCommandMeta() {
        return SimpleCommandMeta.empty();
    }
}
