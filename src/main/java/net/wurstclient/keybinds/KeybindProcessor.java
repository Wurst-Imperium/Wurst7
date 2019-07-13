/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.ClickGuiScreen;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.events.KeyPressListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;

public final class KeybindProcessor implements KeyPressListener
{
	private final HackList hax;
	private final KeybindList keybinds;
	private final CmdProcessor cmdProcessor;
	
	public KeybindProcessor(HackList hax, KeybindList keybinds,
		CmdProcessor cmdProcessor)
	{
		this.hax = hax;
		this.keybinds = keybinds;
		this.cmdProcessor = cmdProcessor;
	}
	
	@Override
	public void onKeyPress(KeyPressEvent event)
	{
		Screen screen = WurstClient.MC.currentScreen;
		if(screen != null && !(screen instanceof ClickGuiScreen))
			return;
		
		if(event.getAction() != GLFW.GLFW_PRESS)
			return;
		
		int keyCode = event.getKeyCode();
		int scanCode = event.getScanCode();
		String keyName = InputUtil.getKeyCode(keyCode, scanCode).getName();
		
		String cmds = keybinds.getCommands(keyName);
		if(cmds == null)
			return;
		
		cmds = cmds.replace(";", "\u00a7").replace("\u00a7\u00a7", ";");
		for(String cmd : cmds.split("\u00a7"))
		{
			cmd = cmd.trim();
			
			if(cmd.startsWith("."))
				cmdProcessor.process(cmd.substring(1));
			else if(cmd.contains(" "))
				cmdProcessor.process(cmd);
			else
			{
				Hack hack = hax.getHackByName(cmd);
				
				if(hack != null)
					hack.setEnabled(!hack.isEnabled());
				else
					cmdProcessor.process(cmd);
			}
		}
	}
}
