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
                ".craft <id>");
    }

    @Override
    public void call(String[] args) throws CmdException
    {
        if (args.length != 1) {
            throw new CmdError("Incorrect number of arguments");
        }
        Identifier itemId = new Identifier("minecraft:" + args[0]);
        if (!Registry.ITEM.containsId(itemId)) {
            throw new CmdError("Item either does not exist or has no recipe");
        }
        autoCraft.queueCraft(itemId);
    }
}
