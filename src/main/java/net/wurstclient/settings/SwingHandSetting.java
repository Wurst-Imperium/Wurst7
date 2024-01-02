/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.Consumer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.wurstclient.WurstClient;

public final class SwingHandSetting
	extends EnumSetting<SwingHandSetting.SwingHand>
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	public SwingHandSetting(String description)
	{
		super("Swing hand", description, SwingHand.values(), SwingHand.SERVER);
	}
	
	public SwingHandSetting(String description, SwingHand selected)
	{
		super("Swing hand", description, SwingHand.values(), selected);
	}
	
	public SwingHandSetting(String name, String description, SwingHand selected)
	{
		super(name, description, SwingHand.values(), selected);
	}
	
	public enum SwingHand
	{
		OFF("Off", hand -> {}),
		
		SERVER("Server-side",
			hand -> MC.player.networkHandler
				.sendPacket(new HandSwingC2SPacket(hand))),
		
		CLIENT("Client-side", hand -> MC.player.swingHand(hand));
		
		private String name;
		private Consumer<Hand> swing;
		
		private SwingHand(String name, Consumer<Hand> swing)
		{
			this.name = name;
			this.swing = swing;
		}
		
		public void swing(Hand hand)
		{
			swing.accept(hand);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
