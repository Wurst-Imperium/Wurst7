/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.render.BufferBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedDeque;

public enum BufferBuilderStorage
{
    ;

    private static final ConcurrentLinkedDeque<BufferBuilder> STORAGE =
        new ConcurrentLinkedDeque<>();

    public static @NotNull BufferBuilder take()
    {
        BufferBuilder buf = STORAGE.pollFirst();
        if(buf == null)
            return new BufferBuilder(256);
        return buf;
    }

    public static void putBack(BufferBuilder buf)
    {
        STORAGE.addFirst(buf);
    }
}
