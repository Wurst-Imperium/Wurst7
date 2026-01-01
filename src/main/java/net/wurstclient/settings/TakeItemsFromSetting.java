/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class TakeItemsFromSetting
	extends EnumSetting<TakeItemsFromSetting.TakeItemsFrom>
{
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final WText REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private TakeItemsFromSetting(String name, WText description,
		TakeItemsFrom[] values, TakeItemsFrom selected)
	{
		super(name, description, values, selected);
	}
	
	public static TakeItemsFromSetting withHands(Hack hack,
		TakeItemsFrom selected)
	{
		return withHands(hackDescription(hack), selected);
	}
	
	public static TakeItemsFromSetting withHands(WText description,
		TakeItemsFrom selected)
	{
		return new TakeItemsFromSetting("Take items from",
			description.append(FULL_DESCRIPTION_SUFFIX), TakeItemsFrom.values(),
			selected);
	}
	
	public static TakeItemsFromSetting withoutHands(Hack hack,
		TakeItemsFrom selected)
	{
		return withoutHands(hackDescription(hack), selected);
	}
	
	public static TakeItemsFromSetting withoutHands(WText description,
		TakeItemsFrom selected)
	{
		TakeItemsFrom[] values =
			{TakeItemsFrom.HOTBAR, TakeItemsFrom.INVENTORY};
		return new TakeItemsFromSetting("Take items from",
			description.append(REDUCED_DESCRIPTION_SUFFIX), values, selected);
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".take_items_from");
	}
	
	public int getMaxInvSlot()
	{
		return getSelected().maxInvSlot;
	}
	
	private static WText buildDescriptionSuffix(boolean includeHands)
	{
		WText text = WText.literal("\n\n");
		TakeItemsFrom[] values =
			includeHands ? TakeItemsFrom.values() : new TakeItemsFrom[]{
				TakeItemsFrom.HOTBAR, TakeItemsFrom.INVENTORY};
		
		for(TakeItemsFrom value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum TakeItemsFrom
	{
		HANDS("Hands", 0),
		HOTBAR("Hotbar", 9),
		INVENTORY("Inventory", 36);
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.take_items_from.";
		
		private final String name;
		private final WText description;
		private final int maxInvSlot;
		
		private TakeItemsFrom(String name, int maxInvSlot)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
			this.maxInvSlot = maxInvSlot;
		}
		
		public int getMaxInvSlot()
		{
			return maxInvSlot;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
