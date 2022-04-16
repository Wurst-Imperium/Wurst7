/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"auto gg", "gg", "good game"})
public final class AutoGGHack extends Hack implements ChatInputListener, UpdateListener
{
	private Pattern chatPattern = Pattern.compile("(\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern teamPattern = Pattern.compile("(\\[TEAM\\] )?(\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern guildPattern = Pattern.compile("Guild > (\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern officerPattern = Pattern.compile("Officer > (\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern partyPattern = Pattern.compile("Party > (\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern shoutPattern = Pattern.compile("(\\[SHOUT\\] )?(\\[.+?\\] )?\\S{1,16}: .*");
	private Pattern spectatorPattern = Pattern.compile("(\\[SPECTATOR\\] )?(\\[.+?\\] )?\\S{1,16}: .*");
		
	private static List<String> triggers = Arrays.asList(
		"1st Killer - ",
		"1st Place - ",
		"Winner: ",
		" - Damage Dealt - ",
		"Winning Team - ",
		"1st - ",
		"WINNER! ",
		"Winners: ",
		"Winner: ",
		"Winning Team: ",
		" won the game!",
		"Top Seeker: ",
		"1st Place: ",
		"Last team standing!",
		"Winner #1 (",
		"Top Survivors",
		"Winners - ");

	private int cooldown = 0;
	
	public AutoGGHack()
	{
		super("AutoGG");
		setCategory(Category.CHAT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		if (cooldown > 0) return;

		String message = event.getComponent().getString();
		if (isNormalMessage(message)) return;
		if (!triggers.stream().anyMatch(message::contains)) return;

		cooldown = 40;
		MC.player.sendChatMessage("/achat gg");
	}

	@Override
	public void onUpdate()
	{
		if (cooldown > 0) cooldown--;
	}

	private boolean isNormalMessage(String message) {
		return this.chatPattern.matcher(message).matches()
			|| this.teamPattern.matcher(message).matches()
			|| this.guildPattern.matcher(message).matches()
			|| this.officerPattern.matcher(message).matches()
			|| this.partyPattern.matcher(message).matches()
			|| this.shoutPattern.matcher(message).matches()
			|| this.spectatorPattern.matcher(message).matches();
	}
}
