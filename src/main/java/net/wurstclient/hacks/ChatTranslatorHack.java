/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.regex.Pattern;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.LanguageSetting;
import net.wurstclient.settings.LanguageSetting.Language;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.GoogleTranslate;

@SearchTags({"chat translator", "ChatTranslate", "chat translate",
	"ChatTranslation", "chat translation", "AutoTranslate", "auto translate",
	"AutoTranslator", "auto translator", "AutoTranslation", "auto translation",
	"GoogleTranslate", "google translate", "GoogleTranslator",
	"google translator", "GoogleTranslation", "google translation"})
public final class ChatTranslatorHack extends Hack implements ChatInputListener
{
	private final LanguageSetting translateFrom =
		LanguageSetting.withAutoDetect("Translate from", Language.AUTO_DETECT);
	
	private final LanguageSetting translateTo =
		LanguageSetting.withoutAutoDetect("Translate to", Language.ENGLISH);
	
	private final CheckboxSetting filterOwnMessages = new CheckboxSetting(
		"Filter own messages",
		"Won't translate messages that appear to be sent by you.\n"
			+ "It tries to detect your messages based on common chat formats"
			+ " like \"<username>\", \"[username]\", or \"username:\". This"
			+ " might not work correctly on some servers.",
		true);
	
	public ChatTranslatorHack()
	{
		super("ChatTranslator");
		setCategory(Category.CHAT);
		addSetting(translateFrom);
		addSetting(translateTo);
		addSetting(filterOwnMessages);
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
		String message = event.getComponent().getString();
		Language fromLang = translateFrom.getSelected();
		Language toLang = translateTo.getSelected();
		
		if(message.startsWith(ChatUtils.WURST_PREFIX)
			|| message.startsWith(toLang.getPrefix()))
			return;
		
		if(filterOwnMessages.isChecked() && isOwnMessage(message))
			return;
		
		Thread.ofVirtual().name("ChatTranslator")
			.uncaughtExceptionHandler((t, e) -> e.printStackTrace())
			.start(() -> showTranslated(message, fromLang, toLang));
	}
	
	private boolean isOwnMessage(String message)
	{
		// Escape username for regex
		String playerName = Pattern.quote(MC.getSession().getUsername());
		
		// Allow up to 2 ranks before the username
		String rankPattern = "(?:\\[[^\\]]+\\] ?){0,2}";
		
		// Build regex and check if it matches
		String regex = "^" + rankPattern + "[<\\[]?" + playerName + "[>\\]:]";
		return Pattern.compile(regex).matcher(message).find();
	}
	
	private void showTranslated(String message, Language fromLang,
		Language toLang)
	{
		String translated = GoogleTranslate.translate(message,
			fromLang.getValue(), toLang.getValue());
		
		if(translated != null)
			MC.inGameHud.getChatHud().addMessage(toLang.prefixText(translated));
	}
}
