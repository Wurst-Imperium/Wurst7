/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class SwingHandSetting
	extends EnumSetting<SwingHandSetting.SwingHand>
{
	private static final MinecraftClient MC = WurstClient.MC;
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final WText REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private SwingHandSetting(WText description, SwingHand[] values,
		SwingHand selected)
	{
		super("Swing hand", description, values, selected);
	}
	
	public SwingHandSetting(WText description, SwingHand selected)
	{
		this(description.append(FULL_DESCRIPTION_SUFFIX), SwingHand.values(),
			selected);
	}
	
	public SwingHandSetting(Hack hack, SwingHand selected)
	{
		this(hackDescription(hack), selected);
	}
	
	public static SwingHandSetting withoutOffOption(WText description,
		SwingHand selected)
	{
		SwingHand[] values = {SwingHand.SERVER, SwingHand.CLIENT};
		return new SwingHandSetting(
			description.append(REDUCED_DESCRIPTION_SUFFIX), values, selected);
	}
	
	public static SwingHandSetting withoutOffOption(Hack hack,
		SwingHand selected)
	{
		return withoutOffOption(hackDescription(hack), selected);
	}
	
	public static WText genericMiningDescription(Hack hack)
	{
		return WText.translated(
			"description.wurst.setting.generic.swing_hand_mining",
			hack.getName());
	}
	
	public static WText genericCombatDescription(Hack hack)
	{
		return WText.translated(
			"description.wurst.setting.generic.swing_hand_combat",
			hack.getName());
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".swing_hand");
	}
	
	public void swing(Hand hand)
	{
		getSelected().swing(hand);
	}
	
	private static WText buildDescriptionSuffix(boolean includeOff)
	{
		WText text = WText.literal("\n\n");
		SwingHand[] values = includeOff ? SwingHand.values()
			: new SwingHand[]{SwingHand.SERVER, SwingHand.CLIENT};
		
		for(SwingHand value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum SwingHand
	{
		OFF("Off", hand -> {}),
		
		SERVER("Server-side",
			hand -> MC.player.networkHandler
				.sendPacket(new HandSwingC2SPacket(hand))),
		
		CLIENT("Client-side", hand -> MC.player.swingHand(hand));
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.swing_hand.";
		
		private final String name;
		private final WText description;
		private final Consumer<Hand> swing;
		
		private SwingHand(String name, Consumer<Hand> swing)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
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
