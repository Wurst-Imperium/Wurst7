/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui;

import java.util.stream.Stream;

import net.wurstclient.Feature;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.Setting;

public final class SettingsWindow extends Window
{
	public SettingsWindow(Feature feature, Window parent, int buttonY)
	{
		super(feature.getName() + " Settings");
		
		Stream<Setting> settings = feature.getSettings().values().stream();
		settings.map(Setting::getComponent).forEach(c -> add(c));
		
		setClosable(true);
		setMinimizable(false);
		pack();
		
		setInitialPosition(parent, buttonY);
	}
	
	private void setInitialPosition(Window parent, int buttonY)
	{
		int scroll = parent.isScrollingEnabled() ? parent.getScrollOffset() : 0;
		int x = parent.getX() + parent.getWidth() + 5;
		int y = parent.getY() + 12 + buttonY + scroll;
		
		net.minecraft.client.util.Window mcWindow = WurstClient.MC.getWindow();
		if(x + getWidth() > mcWindow.getScaledWidth())
			x = parent.getX() - getWidth() - 5;
		if(y + getHeight() > mcWindow.getScaledHeight())
			y -= getHeight() - 14;
		
		setX(x);
		setY(y);
	}
}
