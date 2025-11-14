/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
			"description.wurst.setting.chattranslator.your_language",
			Language.ENGLISH);
	
	private final LanguageSetting otherLanguage =
		LanguageSetting.withoutAutoDetect("Other language",
			"description.wurst.setting.chattranslator.other_language",
			Language.CHINESE_SIMPLIFIED);
	
	private final CheckboxSetting autoDetectReceived =
		new CheckboxSetting("Detect received language",
			"description.wurst.setting.chattranslator.detect_received_language",
			true);
	
	private final CheckboxSetting autoDetectSent = new CheckboxSetting(
		"Detect sent language",
		"description.wurst.setting.chattranslator.detect_sent_language", true);
	
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
			MC.gui.getChat().addMessage(toLang.prefixText(translated));
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
		
		MC.getConnection().sendChat(translated);
	}
}
