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
	private static final String DESCRIPTION_SUFFIX = buildDescriptionSuffix();
	
	public SwingHandSetting(String description)
	{
		this(description, SwingHand.SERVER);
	}
	
	public SwingHandSetting(String description, SwingHand selected)
	{
		this("Swing hand", description, selected);
	}
	
	public SwingHandSetting(String name, String description, SwingHand selected)
	{
		super(name, description + DESCRIPTION_SUFFIX, SwingHand.values(),
			selected);
	}
	
	public void swing(Hand hand)
	{
		getSelected().swing(hand);
	}
	
	private static String buildDescriptionSuffix()
	{
		StringBuilder builder = new StringBuilder("\n\n");
		
		for(SwingHand value : SwingHand.values())
			builder.append("\u00a7l").append(value.name).append("\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return builder.toString();
	}
	
	public enum SwingHand
	{
		OFF("Off",
			"Don't swing your hand at all. Will be detected by anti-cheat"
				+ " plugins.",
			hand -> {}),
		
		SERVER("Server-side",
			"Swing your hand on the server-side, without playing the animation"
				+ " on the client-side.",
			hand -> MC.player.networkHandler
				.sendPacket(new HandSwingC2SPacket(hand))),
		
		CLIENT("Client-side",
			"Swing your hand on the client-side. This is the most legit option.",
			hand -> MC.player.swingHand(hand));
		
		private final String name;
		private final String description;
		private final Consumer<Hand> swing;
		
		private SwingHand(String name, String description, Consumer<Hand> swing)
		{
			this.name = name;
			this.description = description;
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
