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
	private static final String FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final String REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private SwingHandSetting(String name, String description,
		SwingHand[] values, SwingHand selected)
	{
		super(name, description, values, selected);
	}
	
	public SwingHandSetting(String name, String description, SwingHand selected)
	{
		this(name, description + FULL_DESCRIPTION_SUFFIX, SwingHand.values(),
			selected);
	}
	
	public SwingHandSetting(String description, SwingHand selected)
	{
		this("Swing hand", description, selected);
	}
	
	public SwingHandSetting(String description)
	{
		this(description, SwingHand.SERVER);
	}
	
	public static SwingHandSetting withoutOffOption(String name,
		String description, SwingHand selected)
	{
		SwingHand[] values = {SwingHand.SERVER, SwingHand.CLIENT};
		return new SwingHandSetting(name,
			description + REDUCED_DESCRIPTION_SUFFIX, values, selected);
	}
	
	public static SwingHandSetting withoutOffOption(String description,
		SwingHand selected)
	{
		return withoutOffOption("Swing hand", description, selected);
	}
	
	public SwingHand getCurrentSwingHandOption()
	{
		return super.getSelected();
	}
	
	public void swing(Hand hand)
	{
		getSelected().swing(hand);
	}
	
	private static String buildDescriptionSuffix(boolean includeOff)
	{
		StringBuilder builder = new StringBuilder("\n\n");
		SwingHand[] values = includeOff ? SwingHand.values()
			: new SwingHand[]{SwingHand.SERVER, SwingHand.CLIENT};
		
		for(SwingHand value : values)
			builder.append("\u00a7l").append(value.name).append("\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return builder.toString();
	}
	
	public enum SwingHand
	{
		OFF("Off", 1,
			"Don't swing your hand at all. Will be detected by anti-cheat"
				+ " plugins.",
			hand -> {}),
		
		SERVER("Server-side", 2,
			"Swing your hand on the server-side, without playing the animation"
				+ " on the client-side.",
			hand -> MC.player.networkHandler
				.sendPacket(new HandSwingC2SPacket(hand))),
		
		CLIENT("Client-side", 3,
			"Swing your hand on the client-side. This is the most legit option.",
			hand -> MC.player.swingHand(hand));
		
		private final String name;
		private final int optionCode;
		private final Consumer<Hand> swing;
		private final String description;
		
		private SwingHand(String name, int optionCode, String description,
			Consumer<Hand> swing)
		{
			this.name = name;
			this.optionCode = optionCode;
			this.description = description;
			this.swing = swing;
		}
		
		public void swing(Hand hand)
		{
			swing.accept(hand);
		}
		
		public int getOptionCode()
		{
			return optionCode;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
