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

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.extra.confirmation.CommandConfirmationManager;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.commands.ImpactorCommandManager;
import net.impactdev.impactor.api.logging.PluginLogger;
import net.impactdev.impactor.api.platform.plugins.PluginMetadata;
import net.impactdev.impactor.api.utility.ExceptionPrinter;
import net.impactdev.impactor.api.utility.printing.PrettyPrinter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractCommandManager<S> implements ImpactorCommandManager {

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
    private final CommandConfirmationManager<CommandSource> confirmations;

    public AbstractCommandManager(PluginMetadata metadata, PluginLogger logger) {
        this.metadata = metadata;
        this.logger = logger;

        CommandCoordinator coordinator = AsynchronousCommandExecutionCoordinator.<CommandSource>newBuilder()
                .withExecutor(EXECUTOR)
                .withAsynchronousParsing()
                .build()
                ::apply;

        this.manager = this.create(coordinator);
        this.confirmations = new CommandConfirmationManager<>(
                30L,
                TimeUnit.SECONDS,
                context -> context.getCommandContext().getSender().sendMessage(Component.text("Click to confirm action!").color(NamedTextColor.YELLOW)),
                sender -> sender.sendMessage(Component.text("No pending confirmations available...").color(NamedTextColor.RED))
        );

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
    public CommandConfirmationManager<CommandSource> confirmations() {
        return this.confirmations;
    }

    protected abstract CommandManager<CommandSource> create(CommandCoordinator coordinator);

    protected abstract SourceTranslator<S, CommandSource> impactor();

    protected abstract SourceTranslator<CommandSource, S> platform();

    protected abstract void initialize$child();

    protected void initialize() {
        try {
            this.manager.registerExceptionHandler(CommandExecutionException.class, (source, e) -> {
                Component detail = Component.text("An internal error occurred while attempting to perform this command...");
                detail = detail.color(NamedTextColor.RED);

                Style style = detail.style();

                final StringWriter writer = new StringWriter();
                e.getCause().printStackTrace(new PrintWriter(writer));

                final String trace = writer.toString().replace("\t", Strings.repeat(" ", 4));
                final Component result = Component.text(trace).append(Component.newline()).append(
                        Component.text("Click to copy!").color(NamedTextColor.YELLOW)
                );

                style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, result))
                        .clickEvent(ClickEvent.copyToClipboard(trace));

                detail = detail.style(style);
                source.sendMessage(detail);

                this.printException(e);
            });

            this.confirmations.registerConfirmationProcessor(this.manager);
            this.initialize$child();
        } catch (Exception e) {
            ExceptionPrinter.print(this.logger, e);
        }
    }

    private void printException(CommandExecutionException exception) {
        PrettyPrinter printer = new PrettyPrinter(80).wrapTo(80);
        printer.title("Command Execution Exception")
                .add("An unexpected error was encountered during command processing. This error")
                .consume(p -> {
                    String contextual = exception.getCommandContext() != null ? "alongside its relative context" : "";
                    p.add(contextual + " will now be displayed.");
                })
                .hr('-')
                .consume(p -> {
                    final @Nullable CommandContext<?> context = exception.getCommandContext();
                    if (context != null) {
                        p.add("Command Input: %s", context.getRawInputJoined());
                        p.add("During Suggestions: %b", context.isSuggestions());

                        p.add("Context:");
                        context.asMap()
                                .entrySet()
                                .stream()
                                .filter(x -> true)
                                .forEach(entry -> p.add("  %s: %s",
                                        entry.getKey(),
                                        entry.getValue().toString()));
                        p.newline();
                    }
                })
                .add("Encountered Exception Stacktrace:")
                .add(exception);

        printer.log(this.logger, PrettyPrinter.Level.ERROR);
    }
}
