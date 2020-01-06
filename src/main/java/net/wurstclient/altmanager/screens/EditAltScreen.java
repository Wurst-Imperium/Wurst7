/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.altmanager.screens;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.wurstclient.altmanager.Alt;
import net.wurstclient.altmanager.AltManager;

public final class EditAltScreen extends AltEditorScreen
{
	private final AltManager altManager;
	private Alt editedAlt;
	
	public EditAltScreen(Screen prevScreen, AltManager altManager,
		Alt editedAlt)
	{
		super(prevScreen, new LiteralText("Edit Alt"));
		this.altManager = altManager;
		this.editedAlt = editedAlt;
	}
	
	@Override
	protected String getDefaultEmail()
	{
		return editedAlt.getEmail();
	}
	
	@Override
	protected String getDefaultPassword()
	{
		return editedAlt.getPassword();
	}
	
	@Override
	protected String getDoneButtonText()
	{
		return "Save";
	}
	
	@Override
	protected void pressDoneButton()
	{
		altManager.edit(editedAlt, getEmail(), getPassword());
		minecraft.openScreen(prevScreen);
	}
}
