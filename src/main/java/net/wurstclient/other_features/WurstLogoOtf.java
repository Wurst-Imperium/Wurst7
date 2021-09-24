/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.other_features;

import java.util.function.BooleanSupplier;

import net.wurstclient.DontBlock;
import net.wurstclient.SearchTags;
import net.wurstclient.other_feature.OtherFeature;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"wurst logo", "top left corner"})
@DontBlock
public final class WurstLogoOtf extends OtherFeature
{
	private final EnumSetting<Visibility> visibility =
		new EnumSetting<>("能见度", Visibility.values(), Visibility.ALWAYS);
	
	public WurstLogoOtf()
	{
		super("香肠标志", "在屏幕上显示 Wurst 标志和版本.");
		addSetting(visibility);
	}
	
	public boolean isVisible()
	{
		return visibility.getSelected().isVisible();
	}
	
	public static enum Visibility
	{
		ALWAYS("总是", () -> true),
		
		ONLY_OUTDATED("仅当过时",
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
