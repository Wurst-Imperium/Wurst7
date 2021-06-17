/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class NameCmd extends Command
{
    public NameCmd()
    {
        super("name",
                "Shows you or copies the name of your current session.",
                ".name", "Copy to clipboard: .name copy");
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        String name = MC.getSession().getUsername();

        switch(String.join(" ", args).toLowerCase())
        {
            case "":
                ChatUtils.message("Name: " + name);
                break;

            case "copy":
                MC.keyboard.setClipboard(name);
                ChatUtils.message("Name copied to clipboard.");
                break;

            default:
                throw new CmdSyntaxError();
        }
    }

    @Override
    public String getPrimaryAction()
    {
        return "Get Name";
    }

    @Override
    public void doPrimaryAction()
    {
        WURST.getCmdProcessor().process("name");
    }
}
