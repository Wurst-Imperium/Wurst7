/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;

public final class StepHack extends Hack implements UpdateListener
{
	private final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lSimple\u00a7r mode can step up multiple blocks (enables Height slider).\n"
			+ "\u00a7lLegit\u00a7r mode can bypass NoCheat+.",
		Mode.values(), Mode.LEGIT);
	
	private final SliderSetting height =
		new SliderSetting("Height", "Only works in \u00a7lSimple\u00a7r mode.",
			1, 1, 10, 1, ValueDisplay.INTEGER);
	
	public StepHack()
	{
		super("Step");
		setCategory(Category.MOVEMENT);
		addSetting(mode);
		addSetting(height);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(mode.getSelected() == Mode.SIMPLE)
			return;
		
		LocalPlayer player = MC.player;
		if(!player.horizontalCollision)
			return;
		
		if(!player.onGround() || player.onClimbable() || player.isInWater()
			|| player.isInLava())
			return;
		
		if(player.input.getMoveVector().length() <= 1e-5F)
			return;
		
		if(player.jumping)
			return;
		
		AABB box = player.getBoundingBox().move(0, 0.05, 0).inflate(0.05);
		if(!MC.level.noCollision(player, box.move(0, 1, 0)))
			return;
		
		double stepHeight = BlockUtils.getBlockCollisions(box)
			.mapToDouble(bb -> bb.maxY).max().orElse(Double.NEGATIVE_INFINITY);
		
		stepHeight -= player.getY();
		
		if(stepHeight < 0 || stepHeight > 1)
			return;
		
		ClientPacketListener netHandler = player.connection;
		
		netHandler.send(new ServerboundMovePlayerPacket.Pos(player.getX(),
			player.getY() + 0.42 * stepHeight, player.getZ(), player.onGround(),
			MC.player.horizontalCollision));
		
		netHandler.send(new ServerboundMovePlayerPacket.Pos(player.getX(),
			player.getY() + 0.753 * stepHeight, player.getZ(),
			player.onGround(), MC.player.horizontalCollision));
		
		player.setPos(player.getX(), player.getY() + stepHeight, player.getZ());
	}
	
	public float adjustStepHeight(float stepHeight)
	{
		if(isEnabled() && mode.getSelected() == Mode.SIMPLE)
			return height.getValueF();
		
		return stepHeight;
	}
	
	public boolean isAutoJumpAllowed()
	{
		return !isEnabled() && !WURST.getCmds().goToCmd.isActive();
	}
	
	private enum Mode
	{
		SIMPLE("Simple"),
		LEGIT("Legit");
		
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
