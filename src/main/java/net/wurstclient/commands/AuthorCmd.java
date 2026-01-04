/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
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
		
		if(!MC.player.getAbilities().instabuild)
			throw new CmdError("Creative mode only.");
		
		ItemStack heldStack = MC.player.getInventory().getSelectedItem();
		if(!heldStack.is(Items.WRITTEN_BOOK))
			throw new CmdError(
				"You must hold a written book in your main hand.");
		
		WrittenBookContent oldData =
			heldStack.getComponents().get(DataComponents.WRITTEN_BOOK_CONTENT);
		if(oldData == null)
			throw new CmdError("Can't find book data.");
		
		String author = String.join(" ", args);
		WrittenBookContent newData = new WrittenBookContent(oldData.title(),
			author, oldData.generation(), oldData.pages(), oldData.resolved());
		heldStack.set(DataComponents.WRITTEN_BOOK_CONTENT, newData);
	}
}
