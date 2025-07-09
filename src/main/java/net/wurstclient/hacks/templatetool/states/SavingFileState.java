/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.templatetool.states;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.hacks.TemplateToolHack;
import net.wurstclient.hacks.templatetool.TemplateToolState;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonUtils;

public final class SavingFileState extends TemplateToolState
{
	@Override
	protected String getMessage(TemplateToolHack hack)
	{
		return "Saving file...";
	}
	
	@Override
	public void onEnter(TemplateToolHack hack)
	{
		JsonObject json = new JsonObject();
		Direction front = MC.player.getHorizontalFacing();
		Direction left = front.rotateYCounterclockwise();
		BlockPos origin = hack.getOriginPos();
		
		// Add the blocks
		JsonArray jsonBlocks = new JsonArray();
		for(BlockPos pos : hack.getSortedBlocks())
		{
			// Translate
			pos = pos.subtract(origin);
			
			// Rotate
			pos = new BlockPos(0, pos.getY(), 0).offset(front, pos.getZ())
				.offset(left, pos.getX());
			
			// Add to json
			JsonArray xyz = new JsonArray();
			xyz.add(pos.getX());
			xyz.add(pos.getY());
			xyz.add(pos.getZ());
			jsonBlocks.add(xyz);
		}
		json.add("blocks", jsonBlocks);
		
		// Save the file
		try(PrintWriter save = new PrintWriter(new FileWriter(hack.getFile())))
		{
			save.print(JsonUtils.GSON.toJson(json));
			
		}catch(IOException e)
		{
			e.printStackTrace();
			ChatUtils.error("File could not be saved.");
			hack.setEnabled(false);
			return;
		}
		
		// Show success message
		MutableText message = Text.literal("Saved template as ");
		ClickEvent event = new ClickEvent.OpenFile(
			hack.getFile().getParentFile().getAbsolutePath());
		MutableText link = Text.literal(hack.getFile().getName())
			.styled(s -> s.withUnderline(true).withClickEvent(event));
		message.append(link);
		ChatUtils.component(message);
		
		hack.setEnabled(false);
	}
}
