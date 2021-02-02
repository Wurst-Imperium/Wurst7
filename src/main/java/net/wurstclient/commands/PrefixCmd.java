/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */

package net.wurstclient.commands;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

@SearchTags({"change dot", "dots in chat", "command bypass", "prefix"})
public final class PrefixCmd extends Command
{
    public PrefixCmd()
    {
        super("prefix",
                "Change the prefix for all commands.",
                ".prefix <prefix>");
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        if(args.length != 1)
            throw new CmdSyntaxError();

        CmdProcessor.setPrefix(args[0]);

        ChatUtils.message("New prefix is " + "\"" + args[0] + "\"");
    }
}