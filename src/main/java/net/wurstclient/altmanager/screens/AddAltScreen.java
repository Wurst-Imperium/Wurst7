/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.wurstclient.altmanager.AltManager;
import net.wurstclient.altmanager.CrackedAlt;
import net.wurstclient.altmanager.MojangAlt;

public final class AddAltScreen extends AltEditorScreen
{
	private final AltManager altManager;
	
	public AddAltScreen(Screen prevScreen, AltManager altManager)
	{
		super(prevScreen, Component.literal("New Alt"));
		this.altManager = altManager;
	}
	
	@Override
	protected String getDoneButtonText()
	{
		return getPassword().isEmpty() ? "Add Cracked Alt" : "Add Premium Alt";
	}
	
	@Override
	protected void pressDoneButton()
	{
		String nameOrEmail = getNameOrEmail();
		String password = getPassword();
		
		if(password.isEmpty())
			altManager.add(new CrackedAlt(nameOrEmail));
		else
			altManager.add(new MojangAlt(nameOrEmail, password));
		
		minecraft.setScreen(prevScreen);
	}
}
