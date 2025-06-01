/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.wurstclient.Category;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.events.GUIRenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.util.RenderUtils;

public final class TacoCmd extends Command
	implements GUIRenderListener, UpdateListener
{
	private final Identifier[] tacos =
		{Identifier.of("wurst", "dancingtaco1.png"),
			Identifier.of("wurst", "dancingtaco2.png"),
			Identifier.of("wurst", "dancingtaco3.png"),
			Identifier.of("wurst", "dancingtaco4.png")};
	
	private boolean enabled;
	private int ticks = 0;
	
	public TacoCmd()
	{
		super("taco", "Spawns a dancing taco on your hotbar.\n"
			+ "\"I love that little guy. So cute!\" -WiZARD");
		setCategory(Category.FUN);
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 0)
			throw new CmdSyntaxError("Tacos don't need arguments!");
		
		enabled = !enabled;
		
		if(enabled)
		{
			EVENTS.add(GUIRenderListener.class, this);
			EVENTS.add(UpdateListener.class, this);
			
		}else
		{
			EVENTS.remove(GUIRenderListener.class, this);
			EVENTS.remove(UpdateListener.class, this);
		}
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Be a BOSS!";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("taco");
	}
	
	@Override
	public void onUpdate()
	{
		if(ticks >= 31)
			ticks = 0;
		else
			ticks++;
	}
	
	@Override
	public void onRenderGUI(DrawContext context, float partialTicks)
	{
		int color = WURST.getHax().rainbowUiHack.isEnabled()
			? RenderUtils.toIntColor(WURST.getGui().getAcColor(), 1)
			: 0xFFFFFFFF;
		
		int x = context.getScaledWindowWidth() / 2 - 32 + 76;
		int y = context.getScaledWindowHeight() - 32 - 19;
		int w = 64;
		int h = 32;
		context.drawTexture(RenderPipelines.GUI_TEXTURED, tacos[ticks / 8], x,
			y, 0, 0, w, h, w, h, color);
	}
}
