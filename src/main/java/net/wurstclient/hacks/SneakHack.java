/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"AutoSneaking"})
public final class SneakHack extends Hack
	implements PreMotionListener, PostMotionListener
{
	private final EnumSetting<SneakMode> mode = new EnumSetting<>("Mode",
		"\u00a7lPacket\u00a7r mode makes it look like you're sneaking without slowing you down.\n"
			+ "\u00a7lLegit\u00a7r mode actually makes you sneak.",
		SneakMode.values(), SneakMode.LEGIT);
	
	private final CheckboxSetting offWhileFlying =
		new CheckboxSetting("Turn off while flying",
			"Automatically disables Legit Sneak while you are flying or using"
				+ " Freecam, so that it doesn't force you to fly down.\n\n"
				+ "Keep in mind that this also means you won't be hidden from"
				+ " other players while doing these things.",
			false);
	
	public SneakHack()
	{
		super("Sneak");
		setCategory(Category.MOVEMENT);
		addSetting(mode);
		addSetting(offWhileFlying);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + mode.getSelected() + "]";
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		
		switch(mode.getSelected())
		{
			case LEGIT:
			IKeyBinding sneakKey = (IKeyBinding)MC.options.sneakKey;
			sneakKey.resetPressedState();
			break;
			
			case PACKET:
			sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
			break;
		}
	}
	
	@Override
	public void onPreMotion()
	{
		KeyBinding sneakKey = MC.options.sneakKey;
		
		switch(mode.getSelected())
		{
			case LEGIT:
			if(offWhileFlying.isChecked() && isFlying())
				((IKeyBinding)sneakKey).resetPressedState();
			else
				sneakKey.setPressed(true);
			break;
			
			case PACKET:
			((IKeyBinding)sneakKey).resetPressedState();
			sendSneakPacket(Mode.PRESS_SHIFT_KEY);
			sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
			break;
		}
	}
	
	@Override
	public void onPostMotion()
	{
		if(mode.getSelected() != SneakMode.PACKET)
			return;
		
		sendSneakPacket(Mode.RELEASE_SHIFT_KEY);
		sendSneakPacket(Mode.PRESS_SHIFT_KEY);
	}
	
	private boolean isFlying()
	{
		if(MC.player.getAbilities().flying)
			return true;
		
		if(WURST.getHax().flightHack.isEnabled())
			return true;
		
		if(WURST.getHax().freecamHack.isEnabled())
			return true;
		
		return false;
	}
	
	private void sendSneakPacket(Mode mode)
	{
		ClientPlayerEntity player = MC.player;
		ClientCommandC2SPacket packet =
			new ClientCommandC2SPacket(player, mode);
		player.networkHandler.sendPacket(packet);
	}
	
	private enum SneakMode
	{
		PACKET("Packet"),
		LEGIT("Legit");
		
		private final String name;
		
		private SneakMode(String name)
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
