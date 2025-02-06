/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

public final class AuthorCmd extends Command
{
	public AuthorCmd()
	{
		super("author", "Changes the author of a written book.\n"
			+ "Requires creative mode.", ".author <author>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length == 0)
			throw new CmdSyntaxError();
		
		if(!MC.player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		ItemStack heldStack = MC.player.getInventory().getSelectedStack();
		if(!heldStack.isOf(Items.WRITTEN_BOOK))
			throw new CmdError(
				"You must hold a written book in your main hand.");
		
		WrittenBookContentComponent oldData = heldStack.getComponents()
			.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
		if(oldData == null)
			throw new CmdError("Can't find book data.");
		
		String author = String.join(" ", args);
		WrittenBookContentComponent newData =
			new WrittenBookContentComponent(oldData.title(), author,
				oldData.generation(), oldData.pages(), oldData.resolved());
		heldStack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, newData);
	}
}
