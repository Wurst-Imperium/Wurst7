/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;

@SearchTags({"WaterWalking", "water walking"})
public final class JesusHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting bypass =
		new CheckboxSetting("NoCheat+ bypass",
			"Bypasses NoCheat+ but slows down your movement.", false);
	
	private int tickTimer = 10;
	private int packetTimer = 0;
	
	public JesusHack()
	{
		super("Jesus");
		setCategory(Category.MOVEMENT);
		addSetting(bypass);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check if sneaking
		if(MC.options.keyShift.isDown())
			return;
		
		LocalPlayer player = MC.player;
		
		// move up in liquid
		if(player.isInWater() || player.isInLava())
		{
			Vec3 velocity = player.getDeltaMovement();
			player.setDeltaMovement(velocity.x, 0.11, velocity.z);
			tickTimer = 0;
			return;
		}
		
		// simulate jumping out of water
		Vec3 velocity = player.getDeltaMovement();
		if(tickTimer == 0)
			player.setDeltaMovement(velocity.x, 0.30, velocity.z);
		else if(tickTimer == 1)
			player.setDeltaMovement(velocity.x, 0, velocity.z);
		
		// update timer
		tickTimer++;
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof ServerboundMovePlayerPacket))
			return;
		
		ServerboundMovePlayerPacket packet =
			(ServerboundMovePlayerPacket)event.getPacket();
		
		// check if packet contains a position
		if(!(packet instanceof ServerboundMovePlayerPacket.Pos
			|| packet instanceof ServerboundMovePlayerPacket.PosRot))
			return;
		
		// check inWater
		if(MC.player.isInWater())
			return;
		
		// check fall distance
		if(MC.player.fallDistance > 3F)
			return;
		
		if(!isOverLiquid())
			return;
		
		// if not actually moving, cancel packet
		if(MC.player.input == null)
		{
			event.cancel();
			return;
		}
		
		// wait for timer
		packetTimer++;
		if(packetTimer < 4)
			return;
		
		// cancel old packet
		event.cancel();
		
		// get position
		double x = packet.getX(0);
		double y = packet.getY(0);
		double z = packet.getZ(0);
		
		// offset y
		if(bypass.isChecked() && MC.player.tickCount % 2 == 0)
			y -= 0.05;
		else
			y += 0.05;
		
		// create new packet
		Packet<?> newPacket;
		if(packet instanceof ServerboundMovePlayerPacket.Pos)
			newPacket = new ServerboundMovePlayerPacket.Pos(x, y, z, true,
				MC.player.horizontalCollision);
		else
			newPacket = new ServerboundMovePlayerPacket.PosRot(x, y, z,
				packet.getYRot(0), packet.getXRot(0), true,
				MC.player.horizontalCollision);
		
		// send new packet
		MC.player.connection.getConnection().send(newPacket);
	}
	
	public boolean isOverLiquid()
	{
		boolean foundLiquid = false;
		boolean foundSolid = false;
		AABB box = MC.player.getBoundingBox().move(0, -0.5, 0);
		
		// check collision boxes below player
		ArrayList<Block> blockCollisions = BlockUtils.getBlockCollisions(box)
			.map(bb -> BlockUtils.getBlock(BlockPos.containing(bb.getCenter())))
			.collect(Collectors.toCollection(ArrayList::new));
		
		for(Block block : blockCollisions)
			if(block instanceof LiquidBlock)
				foundLiquid = true;
			else if(!(block instanceof AirBlock))
				foundSolid = true;
			
		return foundLiquid && !foundSolid;
	}
	
	public boolean shouldBeSolid()
	{
		return isEnabled() && MC.player != null && MC.player.fallDistance <= 3
			&& !MC.options.keyShift.isDown() && !MC.player.isInWater()
			&& !MC.player.isInLava();
	}
}
