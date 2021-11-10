/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"FastMine", "SpeedMine", "SpeedyGonzales", "fast break",
	"fast mine", "speed mine", "speedy gonzales"})
public final class FastBreakHack extends Hack
	implements BlockBreakingProgressListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		Mode.values(), Mode.NORMAL);
	
	private final SliderSetting speed = new SliderSetting("Speed",
		2, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	public FastBreakHack()
	{
		super("FastBreak");
		setCategory(Category.BLOCKS);
		addSetting(mode);
		addSetting(speed);
	}
	
	@Override
	public String getRenderName()
	{
		switch (mode.getSelected())
		{
			case NORMAL:
			return getName() + " [" + speed.getValueString() + "x]";
			
			default:
			return getName() + " [" + mode.getSelected() + "]";
		}
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(BlockBreakingProgressListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(BlockBreakingProgressListener.class, this);
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		if(mode.getSelected() != Mode.INSTANT)
			return;
		
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		
		if(im.getCurrentBreakingProgress() >= 1)
			return;
		
		Action action = PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK;
		BlockPos blockPos = event.getBlockPos();
		Direction direction = event.getDirection();
		im.sendPlayerActionC2SPacket(action, blockPos, direction);
	}
	
	public float getHardnessModifier()
	{
		return isEnabled() && mode.getSelected() == Mode.NORMAL
			? speed.getValueF() : 1;
	}
	
	private enum Mode
	{
		NORMAL("Normal"),
		INSTANT("Instant");
		
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
