/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.screens.ClickGuiScreen;
import net.wurstclient.command.CmdProcessor;
import net.wurstclient.events.KeyPressListener;
import net.wurstclient.events.MouseButtonPressListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hack.HackList;
import net.wurstclient.util.ChatUtils;

public final class KeybindProcessor
	implements KeyPressListener, MouseButtonPressListener
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
		if(event.getAction() != GLFW.GLFW_PRESS)
			return;
		
		if(!isKeybindProcessingAllowed())
			return;
		
		String keyName = getKeyName(event);
		
		String cmds = keybinds.getCommands(keyName);
		if(cmds == null)
			return;
		
		processCmds(cmds);
	}
	
	@Override
	public void onMouseButtonPress(MouseButtonPressEvent event)
	{
		if(event.getAction() != GLFW.GLFW_PRESS)
			return;
		
		if(!isKeybindProcessingAllowed())
			return;
		
		String keyName = getMouseButtonName(event);
		
		String cmds = keybinds.getCommands(keyName);
		if(cmds == null)
			return;
		
		processCmds(cmds);
	}
	
	private boolean isKeybindProcessingAllowed()
	{
		if(InputConstants.isKeyDown(WurstClient.MC.getWindow(),
			GLFW.GLFW_KEY_F3))
			return false;
		
		Screen screen = WurstClient.MC.screen;
		return screen == null || screen instanceof ClickGuiScreen;
	}
	
	private String getKeyName(KeyPressEvent event)
	{
		int keyCode = event.getKeyCode();
		int scanCode = event.getScanCode();
		return InputConstants
			.getKey(new KeyEvent(keyCode, scanCode, event.getModifiers()))
			.getName();
	}
	
	private String getMouseButtonName(MouseButtonPressEvent event)
	{
		return InputConstants.Type.MOUSE.getOrCreate(event.getButton())
			.getName();
	}
	
	private void processCmds(String cmds)
	{
		cmds = cmds.replace(";", "\u00a7").replace("\u00a7\u00a7", ";");
		
		for(String cmd : cmds.split("\u00a7"))
			processCmd(cmd.trim());
	}
	
	private void processCmd(String cmd)
	{
		if(cmd.startsWith("."))
			cmdProcessor.process(cmd.substring(1));
		else if(cmd.contains(" "))
			cmdProcessor.process(cmd);
		else
		{
			Hack hack = hax.getHackByName(cmd);
			
			if(hack == null)
			{
				cmdProcessor.process(cmd);
				return;
			}
			
			if(!hack.isEnabled() && hax.tooManyHaxHack.isEnabled()
				&& hax.tooManyHaxHack.isBlocked(hack))
			{
				ChatUtils.error(hack.getName() + " is blocked by TooManyHax.");
				return;
			}
			
			hack.setEnabled(!hack.isEnabled());
		}
	}
}
