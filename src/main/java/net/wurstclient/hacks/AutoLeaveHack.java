/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto leave", "AutoDisconnect", "auto disconnect", "AutoQuit",
	"auto quit"})
public final class AutoLeaveHack extends Hack implements UpdateListener
{
	private final SliderSetting health = new SliderSetting("Health",
		"Leaves the server when your health\n"
			+ "reaches this value or falls below it.",
		4, 0.5, 9.5, 0.5,
		v -> ValueDisplay.DECIMAL.getValueString(v) + " hearts");
	
	public final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lQuit\u00a7r mode just quits the game normally.\n"
			+ "Bypasses NoCheat+ but not CombatLog.\n\n"
			+ "\u00a7lChars\u00a7r mode sends a special chat message that\n"
			+ "causes the server to kick you.\n"
			+ "Bypasses NoCheat+ and some versions of CombatLog.\n\n"
			+ "\u00a7lTP\u00a7r mode teleports you to an invalid location,\n"
			+ "causing the server to kick you.\n"
			+ "Bypasses CombatLog, but not NoCheat+.\n\n"
			+ "\u00a7lSelfHurt\u00a7r mode sends the packet for attacking\n"
			+ "another player, but with yourself as both the attacker\n"
			+ "and the target. This causes the server to kick you.\n"
			+ "Bypasses both CombatLog and NoCheat+.",
		Mode.values(), Mode.QUIT);
	
	public AutoLeaveHack()
	{
		super("AutoLeave",
			"Automatically leaves the server\n" + "when your health is low.");
		
		setCategory(Category.COMBAT);
		addSetting(health);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + mode.getSelected() + "]";
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check gamemode
		if(MC.player.abilities.creativeMode)
			return;
		
		// check for other players
		if(MC.isInSingleplayer()
			&& MC.player.networkHandler.getPlayerList().size() == 1)
			return;
		
		// check health
		if(MC.player.getHealth() > health.getValueF() * 2F)
			return;
		
		// leave server
		switch(mode.getSelected())
		{
			case QUIT:
			MC.world.disconnect();
			break;
			
			case CHARS:
			MC.player.networkHandler
				.sendPacket(new ChatMessageC2SPacket("\u00a7"));
			break;
			
			case TELEPORT:
			MC.player.networkHandler.sendPacket(
				new PlayerMoveC2SPacket.PositionOnly(3.1e7, 100, 3.1e7, false));
			break;
			
			case SELFHURT:
			MC.player.networkHandler
				.sendPacket(new PlayerInteractEntityC2SPacket(MC.player,
					MC.player.isSneaking()));
			break;
		}
		
		// disable
		setEnabled(false);
	}
	
	public static enum Mode
	{
		QUIT("Quit"),
		
		CHARS("Chars"),
		
		TELEPORT("TP"),
		
		SELFHURT("SelfHurt");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
