/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */

package net.wurstclient.commands;

import net.wurstclient.SearchTags;
import net.wurstclient.command.*;
import net.wurstclient.util.ChatUtils;

@SearchTags({"change dot", "dots in chat", "command bypass", "prefix"})
public final class PrefixCmd extends Command
{
    public PrefixCmd()
    {
        super("prefix",
                "Change the prefix for all commands.",
                CmdProcessor.getPrefix() + "prefix <prefix>");
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        if(args.length != 1)
            throw new CmdSyntaxError();

        if(!args[0].equals("/")){
            CmdProcessor.setPrefix(args[0]);
        }
        else {
            throw new CmdSyntaxError("Can't use \"/\" because it's the main command prefix of minecraft");
        }

        ChatUtils.message("New prefix is " + "\"" + args[0] + "\"");

        Command.WURST.newCmds();
        Command.WURST.newNavigator();
    }
}