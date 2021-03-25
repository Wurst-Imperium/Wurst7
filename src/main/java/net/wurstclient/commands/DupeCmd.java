/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.packet.c2s.play.BookUpdateC2SPacket;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class DupeCmd extends Command
{
	public DupeCmd()
	{
		super("dupe", "Duplicates items using a book & quill.", ".dupe",
			"How to use:", "1. Put book & quill in main hand.",
			"2. Place items to dupe in your inventory.",
			"3. Disconnect and reconnect.", "4. Place items in a chest.",
			"5. Run this command.");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length > 0)
			throw new CmdSyntaxError();
		
		if(MC.player.inventory.getMainHandStack()
			.getItem() != Items.WRITABLE_BOOK)
		{
			ChatUtils.error("You must hold a book & quill in your main hand.");
			return;
		}
		
		ListTag listTag = new ListTag();
		
		StringBuilder builder1 = new StringBuilder();
		for(int i = 0; i < 21845; i++)
			builder1.append((char)2077);
		
		listTag.addTag(0, StringTag.of(builder1.toString()));
		
		StringBuilder builder2 = new StringBuilder();
		for(int i = 0; i < 32; i++)
			builder2.append("Wurst!!!");
		
		String string2 = builder2.toString();
		for(int i = 1; i < 40; i++)
			listTag.addTag(i, StringTag.of(string2));
		
		ItemStack bookStack = new ItemStack(Items.WRITABLE_BOOK, 1);
		bookStack.putSubTag("title",
			StringTag.of("If you can see this, it didn't work"));
		bookStack.putSubTag("pages", listTag);
		
		MC.player.networkHandler.sendPacket(new BookUpdateC2SPacket(bookStack,
			true, MC.player.inventory.selectedSlot));
	}
}
