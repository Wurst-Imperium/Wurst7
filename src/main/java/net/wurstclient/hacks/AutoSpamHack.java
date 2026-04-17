/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.MessageListSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.text.WText;

@SearchTags({"auto spam", "message spam", "chat spam", "auto message",
	"chat flood", "spammer", "auto command"})
public final class AutoSpamHack extends Hack implements UpdateListener
{
	private static final String FANCY_BLACKLIST = "(){}[]|";
	
	private final MessageListSetting messages =
		new MessageListSetting("Messages",
			WText.translated("description.wurst.setting.autospam.messages"),
			"Wurst on Sauce!");
	
	private final SliderSetting delay =
		new SliderSetting("Delay", "description.wurst.setting.autospam.delay",
			20, 0, 400, 1, ValueDisplay.INTEGER.withSuffix(" ticks"));
	
	private final CheckboxSetting random = new CheckboxSetting("Random order",
		"description.wurst.setting.autospam.random", false);
	
	private final CheckboxSetting fancyFont = new CheckboxSetting("Fancy font",
		"description.wurst.setting.autospam.fancy_font", false);
	
	private int messageIndex;
	private int timer;
	
	public AutoSpamHack()
	{
		super("AutoSpam");
		setCategory(Category.CHAT);
		addSetting(messages);
		addSetting(delay);
		addSetting(random);
		addSetting(fancyFont);
	}
	
	@Override
	protected void onEnable()
	{
		messageIndex = 0;
		timer = delay.getValueI();
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		List<String> msgs = messages.getMessages();
		if(msgs.isEmpty())
			return;
		
		if(timer > 0)
		{
			timer--;
			return;
		}
		
		int i;
		if(random.isChecked())
		{
			i = (int)(Math.random() * msgs.size());
		}else
		{
			if(messageIndex >= msgs.size())
				messageIndex = 0;
			i = messageIndex++;
		}
		
		String msg = msgs.get(i);
		if(msg.length() > 256)
			msg = msg.substring(0, 256);
		
		if(msg.startsWith("/"))
		{
			MC.player.connection.sendCommand(msg.substring(1));
			MC.gui.getChat().addRecentChat("/" + msg.substring(1));
		}else
		
		{
			if(fancyFont.isChecked())
				msg = convertToFancy(msg);
			MC.getConnection().sendChat(msg);
		}
		
		timer = delay.getValueI();
	}
	
	private String convertToFancy(String input)
	{
		StringBuilder output = new StringBuilder();
		for(char c : input.toCharArray())
			output.append(convertChar(c));
		return output.toString();
	}
	
	private String convertChar(char c)
	{
		if(c < 0x21 || c > 0x80)
			return String.valueOf(c);
		if(FANCY_BLACKLIST.contains(Character.toString(c)))
			return String.valueOf(c);
		return new String(Character.toChars(c + 0xfee0));
	}
	
}
