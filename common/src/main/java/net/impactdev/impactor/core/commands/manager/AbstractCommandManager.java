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

package net.impactdev.impactor.core.commands.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.commands.ImpactorCommandManager;
import net.impactdev.impactor.api.commands.RegisterBrigadierMappingsEvent;
import net.impactdev.impactor.api.events.ImpactorEvent;
import net.impactdev.impactor.api.logging.PluginLogger;
import net.impactdev.impactor.api.platform.plugins.PluginMetadata;
import net.impactdev.impactor.api.utility.ExceptionPrinter;
import net.impactdev.impactor.api.utility.printing.PrettyPrinter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.event.EventBus;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;
import org.incendo.cloud.brigadier.BrigadierSetting;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.brigadier.argument.BrigadierMappings;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.AudienceProvider;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.processors.cache.CaffeineCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;
import org.incendo.cloud.setting.Configurable;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

public abstract class AbstractCommandManager implements ImpactorCommandManager {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder()
                    .setNameFormat("Impactor Command Executor: %d")
                    .setDaemon(true)
                    .build()
    );

    protected final PluginMetadata metadata;
    protected final PluginLogger logger;
    private final CommandManager<CommandSource> manager;
    private final ConfirmationManager<CommandSource> confirmations;

    public AbstractCommandManager(PluginMetadata metadata, PluginLogger logger) {
        this.metadata = metadata;
        this.logger = logger;

        ExecutionCoordinator<CommandSource> executor = ExecutionCoordinator.<CommandSource>builder().executor(EXECUTOR).build();
        this.manager = this.create(executor);
        this.confirmations = ConfirmationManager.of(ConfirmationConfiguration.<CommandSource>builder()
                .cache(CaffeineCache.of(Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build()))
                .noPendingCommandNotifier(source -> source.sendMessage(text("No pending confirmations available...").color(NamedTextColor.RED)))
                .confirmationRequiredNotifier((source, context) -> {
                    source.sendMessage(text("Click to confirm action!").color(NamedTextColor.YELLOW));
                })
                .build()
        );
        this.manager.registerCommandPostProcessor(this.confirmations.createPostprocessor());

        if(this.manager instanceof BrigadierManagerHolder<?,?> holder && holder.hasBrigadierManager()) {
            @SuppressWarnings("unchecked")
            CloudBrigadierManager<CommandSource, ?> brigadier = (CloudBrigadierManager<CommandSource, ?>) holder.brigadierManager();

            Configurable<BrigadierSetting> settings = brigadier.settings();
            settings.set(BrigadierSetting.FORCE_EXECUTABLE, true);

            BrigadierMappings<CommandSource, ?> mappings = brigadier.mappings();
            EventBus<ImpactorEvent> events = Impactor.instance().events();

            events.post(new RegisterBrigadierMappingsEvent(mappings));
        }
    }

    @Override
    public PluginMetadata provider() {
        return this.metadata;
    }

    @Override
    public CommandManager<CommandSource> delegate() {
        return this.manager;
    }

    @Override
    public ConfirmationManager<CommandSource> confirmations() {
        return this.confirmations;
    }

    protected abstract CommandManager<CommandSource> create(ExecutionCoordinator<CommandSource> coordinator);

    protected void initialize() {
        try {
            ExceptionController<CommandSource> controller = this.manager.exceptionController();
            controller.registerHandler(CommandExecutionException.class, context -> {
                Component detail = text("An internal error occurred while attempting to perform this command...");
                detail = detail.color(NamedTextColor.RED);

                Style style = detail.style();
                final StringWriter writer = new StringWriter();
                context.exception().getCause().printStackTrace(new PrintWriter(writer));

                final String trace = writer.toString().replace("\t", Strings.repeat(" ", 4));
                final Component result = text(trace).append(Component.newline()).append(
                        text("Click to copy!").color(NamedTextColor.YELLOW)
                );

                style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, result))
                        .clickEvent(ClickEvent.copyToClipboard(trace));

                detail = detail.style(style);

                context.context().sender().sendMessage(detail);
                this.printException(context.exception());
            });

            MinecraftExceptionHandler.<CommandSource>create(AudienceProvider.nativeAudience()).registerTo(this.manager);
        } catch (Exception e) {
            ExceptionPrinter.print(this.logger, e);
        }
    }

    private void printException(CommandExecutionException exception) {
        PrettyPrinter printer = new PrettyPrinter(80).wrapTo(80);
        printer.title("Command Execution Exception")
                .add("An unexpected error was encountered during command processing. This error")
                .consume(p -> {
                    String contextual = exception.context() != null ? "alongside its relative context" : "";
                    p.add(contextual + " will now be displayed.");
                })
                .hr('-')
                .consume(p -> {
                    final @Nullable CommandContext<?> context = exception.context();
                    if (context != null) {
                        p.add("Command Input: %s", context.rawInput().input());
                        p.add("During Suggestions: %b", context.isSuggestions());

                        p.add("Context:");
                        context.all().forEach((key, value) -> p.add("  %s: %s", key, value.toString()));
                        p.newline();
                    }
                })
                .add("Encountered Exception Stacktrace:")
                .add(exception);

        printer.log(this.logger, PrettyPrinter.Level.ERROR);
    }
}
