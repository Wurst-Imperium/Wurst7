/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.settings.SliderSetting;

import java.awt.*;

@SearchTags({"Ingame Background", "Background", "background", "ingame background", "ibackground", "IBackground"})
public final class IngameBackgroundHack extends Hack
{

	private final Color first = new Color(-1072689136, true);
	private final Color second = new Color(-804253680, true);

	public final CheckboxSetting remove = new CheckboxSetting("Remove", "If activated, the in-game background is completely removed.", false);

	public final ColorSetting firstColor = new ColorSetting("First Color", first);
	public final ColorSetting secondColor = new ColorSetting("Second Color", second);

	public final SliderSetting firstAlpha = new SliderSetting("First Alpha", "The alpha of the first colour", 100, 1, 255, 1, SliderSetting.ValueDisplay.INTEGER);
	public final SliderSetting secondAlpha = new SliderSetting("First Alpha", "The alpha of the second colour", 100, 1, 255, 1, SliderSetting.ValueDisplay.INTEGER);

	public IngameBackgroundHack()
	{
		super("IBackground");
		this.setCategory(Category.RENDER);

		this.addSetting(this.remove);
		this.addSetting(this.firstColor);
		this.addSetting(this.secondColor);
	}

	public int firstColor()
	{
		return new Color(this.firstColor.getRed(), this.firstColor.getGreen(), this.firstColor.getBlue(), this.firstAlpha.getValueI()).getRGB();
	}


	public int secondColor()
	{
		return new Color(this.secondColor.getRed(), this.secondColor.getGreen(), this.secondColor.getBlue(), this.secondAlpha.getValueI()).getRGB();
	}

	// See ScreenMixin.onRenderBackground()
	// NOTE: The colours are also from the Minecraft Code
}
