/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.LiteralText;
import net.wurstclient.altmanager.LoginManager;

public final class DirectLoginScreen extends AltEditorScreen
{
	public DirectLoginScreen(Screen prevScreen)
	{
		super(prevScreen, new LiteralText("Direct Login"));
	}
	
	@Override
	protected String getDoneButtonText()
	{
		return "Login";
	}
	
	@Override
	protected void pressDoneButton()
	{
		if(getPassword().isEmpty())
		{
			message = "";
			LoginManager.changeCrackedName(getEmail());
			
		}else
			message = LoginManager.login(getEmail(), getPassword());
		
		if(message.isEmpty())
			client.openScreen(new TitleScreen());
		else
			doErrorEffect();
	}
}
