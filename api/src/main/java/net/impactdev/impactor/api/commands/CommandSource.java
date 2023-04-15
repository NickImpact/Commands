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

package net.impactdev.impactor.api.commands;

import net.impactdev.impactor.api.platform.audience.LocalizedAudience;
import net.impactdev.impactor.api.platform.players.PlatformPlayer;
import net.impactdev.impactor.api.platform.sources.PlatformSource;
import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Represents the source of a command. A command source need not be a player, but simply any sort of
 * entity that is capable of executing commands, such as a command block, a book, a sign, or the server
 * itself. Any source must be identifiable from a particular UUID and should be capable of receiving
 */
public interface CommandSource extends LocalizedAudience {

    /**
     * Represents the unique identifier of the source of a command. This is meant to simply help
     * indicate who is responsible for a command's execution, and will not necessarily represent
     * a player.
     *
     * @return The unique identifier representing the source of a command
     */
    UUID uuid();

    /**
     * Represents a human-readable component detailing the name of the source of a command.
     *
     * @return The name of the command source
     */
    Component name();

    /**
     * Represents a basic platform source that wraps further information regarding the command
     * source. Internally, all command sources will be backed by a platform source, and this call
     * will ultimately never fail to produce a subject.
     *
     * @return A platform-agnostic source type representing further information about this particular
     * command source
     */
    PlatformSource source();

    /**
     * Provides the command source as a {@link PlatformPlayer}, so long as it's a valid conversion. If the
     * command source is not a player, this will provoke an exception to indicate such.
     *
     * @return A platform-agnostic player representing the command source
     * @throws IllegalStateException If the source is not a player, but some other form of entity
     */
    PlatformPlayer player();

}
