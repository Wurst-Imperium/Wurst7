/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.text.Text;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.GoogleTranslate;

@SearchTags({"chat translator", "ChatTranslate", "chat translate",
	"ChatTranslation", "chat translation", "AutoTranslate", "auto translate",
	"AutoTranslator", "auto translator", "AutoTranslation", "auto translation",
	"GoogleTranslate", "google translate", "GoogleTranslator",
	"google translator", "GoogleTranslation", "google translation"})
public final class ChatTranslatorHack extends Hack implements ChatInputListener
{
	private final EnumSetting<FromLanguage> langFrom = new EnumSetting<>(
		"Translate from", FromLanguage.values(), FromLanguage.AUTO_DETECT);
	
	private final EnumSetting<ToLanguage> langTo = new EnumSetting<>(
		"Translate to", ToLanguage.values(), ToLanguage.ENGLISH);
	
	public ChatTranslatorHack()
	{
		super("ChatTranslator");
		setCategory(Category.CHAT);
		
		addSetting(langFrom);
		addSetting(langTo);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> translate(event.getComponent().getString()));
	}
	
	private void translate(String message)
	{
		String prefix =
			"\u00a7a[\u00a7b" + langTo.getSelected().name + "\u00a7a]:\u00a7r ";
		
		if(message.startsWith(ChatUtils.WURST_PREFIX)
			|| message.startsWith(prefix))
			return;
		
		String translated = GoogleTranslate.translate(message,
			langFrom.getSelected().value, langTo.getSelected().value);
		
		if(translated == null)
			return;
		
		MC.inGameHud.getChatHud().addMessage(Text.literal(prefix + translated));
	}
	
	public static enum FromLanguage
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
		
		private FromLanguage(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	public static enum ToLanguage
	{
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
		
		private ToLanguage(String name, String value)
		{
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
