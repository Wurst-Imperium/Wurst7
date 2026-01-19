/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.freecam;

import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.text.WText;

public final class FreecamInputSetting
	extends EnumSetting<FreecamInputSetting.ApplyInputTo>
{
	private static final WText DESCRIPTION = buildDescription();
	
	public FreecamInputSetting()
	{
		super("Apply input to", DESCRIPTION, ApplyInputTo.values(),
			ApplyInputTo.CAMERA);
	}
	
	private static WText buildDescription()
	{
		WText text = WText
			.translated("description.wurst.setting.freecam.apply_input_to");
		
		for(ApplyInputTo value : ApplyInputTo.values())
			text = text
				.append(WText.literal("\n\n\u00a7l" + value.name + ":\u00a7r "))
				.append(value.description);
		
		return text;
	}
	
	public enum ApplyInputTo
	{
		CAMERA("Camera"),
		PLAYER("Player");
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.freecam.apply_input_to.";
		
		private final String name;
		private final WText description;
		
		private ApplyInputTo(String name)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
