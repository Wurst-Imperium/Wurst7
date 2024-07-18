/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import java.util.Objects;

import net.wurstclient.clickgui.screens.EditBookOffersScreen;
import net.wurstclient.settings.BookOffersSetting;
import net.wurstclient.settings.Setting;

public final class BookOffersEditButton extends AbstractListEditButton
{
	private final BookOffersSetting setting;
	
	public BookOffersEditButton(BookOffersSetting setting)
	{
		this.setting = Objects.requireNonNull(setting);
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	protected void openScreen()
	{
		MC.setScreen(new EditBookOffersScreen(MC.currentScreen, setting));
	}
	
	@Override
	protected String getText()
	{
		return setting.getName() + ": " + setting.getOffers().size();
	}
	
	@Override
	protected Setting getSetting()
	{
		return setting;
	}
}
