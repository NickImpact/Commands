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

package net.impactdev.impactor.api.commands.events;

import net.impactdev.impactor.api.commands.CommandSource;
import net.impactdev.impactor.api.events.ImpactorEvent;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.Requirement;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;
import org.incendo.cloud.processors.requirements.annotation.RequirementBindings;

import java.lang.annotation.Annotation;
import java.util.function.Function;

public record RegisterCommandRequirement(
        CommandManager<CommandSource> manager,
        AnnotationParser<CommandSource> parser
) implements ImpactorEvent
{

    public <T extends Requirement<CommandSource, T>> void register(final RequirementPostprocessor<CommandSource, T> postprocessor) {
        this.manager.registerCommandPostProcessor(postprocessor);
    }

    public <T extends Requirement<CommandSource, T>, A extends Annotation> void bind(
            CloudKey<Requirements<CommandSource, T>> key,
            Class<A> annotation,
            Function<A, T> provider
    ) {
        RequirementBindings.create(this.parser, key).register(annotation, provider);
    }
}
