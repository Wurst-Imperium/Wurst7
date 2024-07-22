/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.chattranslator.FilterOwnMessagesSetting;
import net.wurstclient.hacks.chattranslator.GoogleTranslate;
import net.wurstclient.hacks.chattranslator.LanguageSetting;
import net.wurstclient.hacks.chattranslator.LanguageSetting.Language;
import net.wurstclient.hacks.chattranslator.WhatToTranslateSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.ChatUtils;

@SearchTags({"chat translator", "ChatTranslate", "chat translate",
	"ChatTranslation", "chat translation", "AutoTranslate", "auto translate",
	"AutoTranslator", "auto translator", "AutoTranslation", "auto translation",
	"GoogleTranslate", "google translate", "GoogleTranslator",
	"google translator", "GoogleTranslation", "google translation"})
public final class ChatTranslatorHack extends Hack
	implements ChatInputListener, ChatOutputListener
{
	private final WhatToTranslateSetting whatToTranslate =
		new WhatToTranslateSetting();
	
	private final LanguageSetting playerLanguage =
		LanguageSetting.withoutAutoDetect("Your language",
			"The main language that you can use and understand.\n\n"
				+ "Your received messages will always be translated into this"
				+ " language (if enabled).\n\n"
				+ "When \"Detect sent language\" is turned off, all"
				+ " sent messages are assumed to be in this language.",
			Language.ENGLISH);
	
	private final LanguageSetting otherLanguage =
		LanguageSetting.withoutAutoDetect("Other language",
			"The main language used by other players on the server.\n\n"
				+ "Your sent messages will always be translated into this"
				+ " language (if enabled).\n\n"
				+ "When \"Detect received language\" is turned off, all"
				+ " received messages are assumed to be in this language.",
			Language.CHINESE_SIMPLIFIED);
	
	private final CheckboxSetting autoDetectReceived =
		new CheckboxSetting("Detect received language",
			"Automatically detect the language of received messages.\n\n"
				+ "Useful if other players are using a mix of different"
				+ " languages.\n\n"
				+ "If everyone is using the same language, turning this off"
				+ " can improve accuracy.",
			true);
	
	private final CheckboxSetting autoDetectSent =
		new CheckboxSetting("Detect sent language",
			"Automatically detect the language of sent messages.\n\n"
				+ "Useful if you're using a mix of different languages.\n\n"
				+ "If you're always using the same language, turning this off"
				+ " can improve accuracy.",
			true);
	
	private final FilterOwnMessagesSetting filterOwnMessages =
		new FilterOwnMessagesSetting();
	
	public ChatTranslatorHack()
	{
		super("ChatTranslator");
		setCategory(Category.CHAT);
		addSetting(whatToTranslate);
		addSetting(playerLanguage);
		addSetting(otherLanguage);
		addSetting(autoDetectReceived);
		addSetting(autoDetectSent);
		addSetting(filterOwnMessages);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
		EVENTS.add(ChatOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		EVENTS.remove(ChatOutputListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		if(!whatToTranslate.includesReceived())
			return;
		
		String message = event.getComponent().getString();
		Language fromLang = autoDetectReceived.isChecked()
			? Language.AUTO_DETECT : otherLanguage.getSelected();
		Language toLang = playerLanguage.getSelected();
		
		if(message.startsWith(ChatUtils.WURST_PREFIX)
			|| message.startsWith(toLang.getPrefix()))
			return;
		
		if(filterOwnMessages.isChecked()
			&& filterOwnMessages.isOwnMessage(message))
			return;
		
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> showTranslated(message, fromLang, toLang));
	}
	
	private void showTranslated(String message, Language fromLang,
		Language toLang)
	{
		String translated = GoogleTranslate.translate(message,
			fromLang.getValue(), toLang.getValue());
		
		if(translated != null)
			MC.inGameHud.getChatHud().addMessage(toLang.prefixText(translated));
	}
	
	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		if(!whatToTranslate.includesSent())
			return;
		
		String message = event.getMessage();
		Language fromLang = autoDetectSent.isChecked() ? Language.AUTO_DETECT
			: playerLanguage.getSelected();
		Language toLang = otherLanguage.getSelected();
		
		if(message.startsWith("/") || message.startsWith("."))
			return;
		
		event.cancel();
		
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> sendTranslated(message, fromLang, toLang));
	}
	
	private void sendTranslated(String message, Language fromLang,
		Language toLang)
	{
		String translated = GoogleTranslate.translate(message,
			fromLang.getValue(), toLang.getValue());
		
		if(translated == null)
			translated = message;
		
		MC.getNetworkHandler().sendChatMessage(translated);
	}
}
