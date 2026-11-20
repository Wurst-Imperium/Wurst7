/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PostMotionListener;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyMapping;
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
	
	private boolean packetSneaking;
	
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
	protected void onEnable()
	{
		EVENTS.add(PreMotionListener.class, this);
		EVENTS.add(PostMotionListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PreMotionListener.class, this);
		EVENTS.remove(PostMotionListener.class, this);
		
		IKeyMapping.get(MC.options.keyShift).resetDownState();
		stopPacketSneaking();
	}
	
	@Override
	public void onPreMotion()
	{
		if(MC.player.isPassenger())
			return;
		
		IKeyMapping sneakKey = IKeyMapping.get(MC.options.keyShift);
		
		switch(mode.getSelected())
		{
			case LEGIT:
			if(offWhileFlying.isChecked() && isFlying())
				sneakKey.resetDownState();
			else
				sneakKey.setDown(true);
			break;
			
			case PACKET:
			sneakKey.resetDownState();
			sendSneakPacket(true);
			sendSneakPacket(false);
			break;
		}
	}
	
	@Override
	public void onPostMotion()
	{
		if(mode.getSelected() != SneakMode.PACKET || MC.player.isPassenger())
		{
			stopPacketSneaking();
			return;
		}
		
		sendSneakPacket(false);
		sendSneakPacket(true);
		packetSneaking = true;
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
	
	private void stopPacketSneaking()
	{
		if(!packetSneaking)
			return;
		
		packetSneaking = false;
		if(MC.player != null)
			sendSneakPacket(MC.player.getLastSentInput().shift());
	}
	
	private void sendSneakPacket(boolean sneaking)
	{
		Input input = MC.player.getLastSentInput();
		Input packetInput =
			new Input(input.forward(), input.backward(), input.left(),
				input.right(), input.jump(), sneaking, input.sprint());
		MC.player.connection
			.send(new ServerboundPlayerInputPacket(packetInput));
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
