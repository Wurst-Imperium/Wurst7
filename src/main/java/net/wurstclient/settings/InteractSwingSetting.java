/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.function.BiConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class InteractSwingSetting
	extends EnumSetting<InteractSwingSetting.InteractSwing>
{
	private static final Minecraft MC = WurstClient.MC;
	private static final WText DESCRIPTION_SUFFIX = buildDescriptionSuffix();
	
	public InteractSwingSetting(WText description, InteractSwing selected)
	{
		super("Interact swing", description.append(DESCRIPTION_SUFFIX),
			InteractSwing.values(), selected);
	}
	
	public InteractSwingSetting(Hack hack, InteractSwing selected)
	{
		this(hackDescription(hack), selected);
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".interact_swing");
	}
	
	public void swing(InteractionHand hand)
	{
		getSelected().swing(hand);
	}
	
	public void swing(InteractionHand hand, SwingAnimation animation)
	{
		getSelected().swing(hand, animation);
	}
	
	private static WText buildDescriptionSuffix()
	{
		WText text = WText.literal("\n\n");
		
		for(InteractSwing value : InteractSwing.values())
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum InteractSwing
	{
		CLIENT("Client-side",
			(hand, animation) -> MC.player.swing(hand, animation, false)),
		
		SERVER("Server-side", (hand, animation) -> {});
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.swing_hand.";
		
		private final String name;
		private final WText description;
		private final BiConsumer<InteractionHand, SwingAnimation> swing;
		
		private InteractSwing(String name,
			BiConsumer<InteractionHand, SwingAnimation> swing)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
			this.swing = swing;
		}
		
		public void swing(InteractionHand hand)
		{
			if(this == SERVER)
				return;
			
			SwingAnimation animation =
				MC.player.getItemInHand(hand).getInteractAnimation();
			swing.accept(hand, animation);
		}
		
		public void swing(InteractionHand hand, SwingAnimation animation)
		{
			swing.accept(hand, animation);
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
