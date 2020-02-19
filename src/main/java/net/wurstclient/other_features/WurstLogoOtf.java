/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import java.util.function.BooleanSupplier;

import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"wurst logo", "top left corner"})
public final class WurstLogoOtf extends OtherFeature
{
	private final EnumSetting<Visibility> visibility =
		new EnumSetting<>("Visibility", Visibility.values(), Visibility.ALWAYS);
	
	public WurstLogoOtf()
	{
		super("WurstLogo", "Shows the Wurst logo and version on the screen.");
		addSetting(visibility);
	}
	
	public boolean isVisible()
	{
		return visibility.getSelected().isVisible();
	}
	
	public static enum Visibility
	{
		ALWAYS("Always", () -> true),
		
		ONLY_OUTDATED("Only when outdated",
			() -> WURST.getUpdater().isOutdated());
		
		private final String name;
		private final BooleanSupplier visible;
		
		private Visibility(String name, BooleanSupplier visible)
		{
			this.name = name;
			this.visible = visible;
		}
		
		public boolean isVisible()
		{
			return visible.getAsBoolean();
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
