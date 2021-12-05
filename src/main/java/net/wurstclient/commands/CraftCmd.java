/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.wurstclient.WurstClient;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hacks.AutoCraftHack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.MathUtils;

public final class CraftCmd extends Command
{

    private final AutoCraftHack autoCraft = WurstClient.INSTANCE.getHax().autoCraftHack;

    public CraftCmd()
    {
        super("craft",
                "Adds an item to the AutoCraft crafting queue.\n",
                ".craft <id:count ...>");
    }

    private class ParsedArgument {
        private String id;
        private int count;
        private boolean craftAll;
        public ParsedArgument(String id, int count, boolean craftAll) {
            this.id = id;
            this.count = count;
            this.craftAll = craftAll;
        }
    }

    private ParsedArgument parse(String info) throws CmdException {
        if (info.contains(":")) {
            String[] components = info.split(":");
            String id = components[0];
            int count = 0;
            boolean craftAll = false;
            if (components[1].equals("all")) {
                craftAll = true;
            }
            else {
                try {
                    count = Integer.parseInt(components[1]);
                } catch (NumberFormatException e) {
                    throw new CmdError("Invalid count: " + components[1]);
                }
            }
            return new ParsedArgument(id, count, craftAll);
        }
        return new ParsedArgument(info, 1, false);
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        if (args.length == 0) {
            throw new CmdError("Invalid number of arguments");
        }
        for (int i = 0; i < args.length; i++) {
            ParsedArgument parsed = parse(args[i]);
            Identifier itemId = new Identifier("minecraft", parsed.id);
            if (!Registry.ITEM.containsId(itemId)) {
                throw new CmdError("Item " + itemId + " either does not exist or has no recipe");
            }
        }
        for (int i = 0; i < args.length; i++) {
            ParsedArgument parsed = parse(args[i]);
            Identifier itemId = new Identifier("minecraft:" + parsed.id);
            autoCraft.queueCraft(itemId, parsed.count, parsed.craftAll);
        }
        if (!autoCraft.isEnabled())
            autoCraft.setEnabled(true);
    }
}
