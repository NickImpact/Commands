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

import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.brigadier.CloudBrigadierManager;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.leangen.geantyref.TypeToken;
import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.logging.PluginLogger;
import net.impactdev.impactor.api.utility.ExceptionPrinter;
import net.kyori.adventure.key.Key;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

public final class BrigadierMapper {

    private final CloudBrigadierManager<CommandSource, CommandSourceStack> manager;
    private final PluginLogger logger;
    private final Map<Class<?>, ArgumentTypeInfo<?, ?>> arguments;

    public BrigadierMapper(PluginLogger logger, CloudBrigadierManager<CommandSource, CommandSourceStack> manager) {
        this.manager = manager;
        this.logger = logger;

        try {
            final Field map = ArgumentTypeInfos.class.getDeclaredFields()[0];
            map.setAccessible(true);

            this.arguments = (Map<Class<?>, ArgumentTypeInfo<?,?>>) map.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends ArgumentParser<CommandSource, ?>> void map(TypeToken<T> type, Key key, boolean useCloudSuggestions) {
        final Constructor<?> constructor;
        try {
            final Class<?> target = this.getClassByKey(key);
            constructor = target.getConstructor();
        } catch (final RuntimeException | ReflectiveOperationException e) {
            ExceptionPrinter.print(this.logger, e);
            return;
        }

        this.manager.registerMapping(type, builder -> {
            builder.to(argument -> {
                try {
                    return (ArgumentType<?>) constructor.newInstance();
                } catch (final ReflectiveOperationException e) {
                    ExceptionPrinter.print(this.logger, e);
                    return StringArgumentType.word();
                }
            });
            if(useCloudSuggestions) {
                builder.cloudSuggestions();
            }
        });
    }

    private Class<? extends ArgumentType<?>> getClassByKey(final @NotNull Key key) throws IllegalArgumentException {
        final Registry<ArgumentTypeInfo<?, ?>> registry = Registry.COMMAND_ARGUMENT_TYPE;
        final ArgumentTypeInfo<?, ?> target = registry.get(ResourceLocation.tryParse(key.asString()));

        for(final Map.Entry<?, ?> entry : this.arguments.entrySet()) {
            if(entry.getValue().equals(target)) {
                return (Class<? extends ArgumentType<?>>) entry.getKey();
            }
        }

        throw new IllegalArgumentException("Invalid key: " + key.asString());
    }

}
