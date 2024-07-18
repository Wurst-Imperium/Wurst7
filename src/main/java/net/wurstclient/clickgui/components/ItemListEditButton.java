/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import net.wurstclient.clickgui.screens.EditItemListScreen;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.Setting;

public final class ItemListEditButton extends AbstractListEditButton
{
	private final ItemListSetting setting;
	
	public ItemListEditButton(ItemListSetting setting)
	{
		this.setting = Objects.requireNonNull(setting);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	protected void openScreen()
	{
		MC.setScreen(new EditItemListScreen(MC.currentScreen, setting));
	}
	
	@Override
	protected String getText()
	{
		return setting.getName() + ": " + setting.getItemNames().size();
	}
	
	@Override
	protected Setting getSetting()
	{
		return setting;
	}
}
