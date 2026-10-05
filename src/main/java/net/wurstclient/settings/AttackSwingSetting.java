/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.world.InteractionHand;
import net.wurstclient.WurstClient;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.text.WText;

public final class AttackSwingSetting
	extends EnumSetting<AttackSwingSetting.AttackSwing>
{
	private static final Minecraft MC = WurstClient.MC;
	private static final WText FULL_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(true);
	private static final WText REDUCED_DESCRIPTION_SUFFIX =
		buildDescriptionSuffix(false);
	
	private AttackSwingSetting(WText description, AttackSwing[] values,
		AttackSwing selected)
	{
		super("Attack swing", description, values, selected);
	}
	
	public AttackSwingSetting(WText description, AttackSwing selected)
	{
		this(description.append(FULL_DESCRIPTION_SUFFIX), AttackSwing.values(),
			selected);
	}
	
	public AttackSwingSetting(Hack hack, AttackSwing selected)
	{
		this(hackDescription(hack), selected);
	}
	
	public static AttackSwingSetting withoutOffOption(WText description,
		AttackSwing selected)
	{
		AttackSwing[] values = {AttackSwing.CLIENT, AttackSwing.SERVER};
		return new AttackSwingSetting(
			description.append(REDUCED_DESCRIPTION_SUFFIX), values, selected);
	}
	
	public static AttackSwingSetting withoutOffOption(Hack hack,
		AttackSwing selected)
	{
		return withoutOffOption(hackDescription(hack), selected);
	}
	
	public static WText genericMiningDescription(Hack hack)
	{
		return WText.translated(
			"description.wurst.setting.generic.attack_swing_mining",
			hack.getName());
	}
	
	public static WText genericCombatDescription(Hack hack)
	{
		return WText.translated(
			"description.wurst.setting.generic.attack_swing_combat",
			hack.getName());
	}
	
	private static WText hackDescription(Hack hack)
	{
		return WText.translated("description.wurst.setting."
			+ hack.getName().toLowerCase() + ".attack_swing");
	}
	
	public void swing()
	{
		getSelected().swing();
	}
	
	private static WText buildDescriptionSuffix(boolean includeOff)
	{
		WText text = WText.literal("\n\n");
		AttackSwing[] values = includeOff ? AttackSwing.values()
			: new AttackSwing[]{AttackSwing.CLIENT, AttackSwing.SERVER};
		
		for(AttackSwing value : values)
			text.append("\u00a7l" + value.name + "\u00a7r - ")
				.append(value.description).append("\n\n");
		
		return text;
	}
	
	public enum AttackSwing
	{
		OFF("Off", () -> {}),
		
		CLIENT("Client-side", () -> {
			MC.player.swing(InteractionHand.MAIN_HAND,
				MC.player.getMainHandItem().getAttackAnimation(), false);
			MC.player.connection.send(ServerboundPunchPacket.INSTANCE);
		}),
		
		SERVER("Server-side",
			() -> MC.player.connection.send(ServerboundPunchPacket.INSTANCE));
		
		private static final String TRANSLATION_KEY_PREFIX =
			"description.wurst.setting.generic.swing_hand.";
		
		private final String name;
		private final WText description;
		private final Runnable swing;
		
		private AttackSwing(String name, Runnable swing)
		{
			this.name = name;
			description =
				WText.translated(TRANSLATION_KEY_PREFIX + name().toLowerCase());
			this.swing = swing;
		}
		
		public void swing()
		{
			swing.run();
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
