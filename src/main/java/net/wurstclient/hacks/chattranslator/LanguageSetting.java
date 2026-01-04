/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chattranslator;

import net.minecraft.network.chat.Component;
import net.wurstclient.settings.EnumSetting;

public final class LanguageSetting extends EnumSetting<LanguageSetting.Language>
{
	private LanguageSetting(String name, String description, Language[] values,
		Language selected)
	{
		super(name, description, values, selected);
	}
	
	public static LanguageSetting withAutoDetect(String name,
		String description, Language selected)
	{
		return new LanguageSetting(name, description, Language.values(),
			selected);
	}
	
	public static LanguageSetting withAutoDetect(String name, Language selected)
	{
		return new LanguageSetting(name, "", Language.values(), selected);
	}
	
	public static LanguageSetting withoutAutoDetect(String name,
		String description, Language selected)
	{
		Language[] values = Language.valuesWithoutAutoDetect();
		return new LanguageSetting(name, description, values, selected);
	}
	
	public static LanguageSetting withoutAutoDetect(String name,
		Language selected)
	{
		Language[] values = Language.valuesWithoutAutoDetect();
		return new LanguageSetting(name, "", values, selected);
	}
	
	public enum Language
	{
		AUTO_DETECT("Detect Language", "auto"),
		AFRIKAANS("Afrikaans", "af"),
		ARABIC("Arabic", "ar"),
		CZECH("Czech", "cs"),
		CHINESE_SIMPLIFIED("Chinese (simplified)", "zh-CN"),
		CHINESE_TRADITIONAL("Chinese (traditional)", "zh-TW"),
		DANISH("Danish", "da"),
		DUTCH("Dutch", "nl"),
		ENGLISH("English", "en"),
		FINNISH("Finnish", "fi"),
		FRENCH("French", "fr"),
		GERMAN("Deutsch!", "de"),
		GREEK("Greek", "el"),
		HINDI("Hindi", "hi"),
		ITALIAN("Italian", "it"),
		JAPANESE("Japanese", "ja"),
		KOREAN("Korean", "ko"),
		NORWEGIAN("Norwegian", "no"),
		POLISH("Polish", "pl"),
		PORTUGUESE("Portugese", "pt"),
		RUSSIAN("Russian", "ru"),
		SPANISH("Spanish", "es"),
		SWAHILI("Swahili", "sw"),
		SWEDISH("Swedish", "sv"),
		TURKISH("Turkish", "tr");
		
		private final String name;
		private final String value;
		private final String prefix;
		
		private Language(String name, String value)
		{
			this.name = name;
			this.value = value;
			prefix = "\u00a7a[\u00a7b" + name + "\u00a7a]:\u00a7r ";
		}
		
		public String getValue()
		{
			return value;
		}
		
		public String getPrefix()
		{
			return prefix;
		}
		
		public Component prefixText(String s)
		{
			return Component.literal(prefix + s);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
		
		private static Language[] valuesWithoutAutoDetect()
		{
			Language[] allValues = values();
			Language[] valuesWithoutAuto = new Language[allValues.length - 1];
			System.arraycopy(allValues, 1, valuesWithoutAuto, 0,
				valuesWithoutAuto.length);
			return valuesWithoutAuto;
		}
	}
}
