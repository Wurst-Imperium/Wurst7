/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;

import net.minecraft.block.Material;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
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
		super("Jesus", "Allows you to walk on water.\n"
			+ "Jesus used this hack ~2000 years ago.");
		setCategory(Category.MOVEMENT);
		addSetting(bypass);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// check if sneaking
		if(MC.options.keySneak.isPressed())
			return;
		
		ClientPlayerEntity player = MC.player;
		
		// move up in water
		if(player.isTouchingWater())
		{
			Vec3d velocity = player.getVelocity();
			player.setVelocity(velocity.x, 0.11, velocity.z);
			tickTimer = 0;
			return;
		}
		
		// simulate jumping out of water
		Vec3d velocity = player.getVelocity();
		if(tickTimer == 0)
			player.setVelocity(velocity.x, 0.30, velocity.z);
		else if(tickTimer == 1)
			player.setVelocity(velocity.x, 0, velocity.z);
		
		// update timer
		tickTimer++;
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		// check packet type
		if(!(event.getPacket() instanceof PlayerMoveC2SPacket))
			return;
		
		PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)event.getPacket();
		
		// check if packet contains a position
		if(!(packet instanceof PlayerMoveC2SPacket.PositionOnly
			|| packet instanceof PlayerMoveC2SPacket.Both))
			return;
		
		// check inWater
		if(MC.player.isTouchingWater())
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
		if(bypass.isChecked() && MC.player.age % 2 == 0)
			y -= 0.05;
		else
			y += 0.05;
		
		// create new packet
		Packet<?> newPacket;
		if(packet instanceof PlayerMoveC2SPacket.PositionOnly)
			newPacket = new PlayerMoveC2SPacket.PositionOnly(x, y, z, true);
		else
			newPacket = new PlayerMoveC2SPacket.Both(x, y, z, packet.getYaw(0),
				packet.getPitch(0), true);
		
		// send new packet
		MC.player.networkHandler.getConnection().send(newPacket);
	}
	
	public boolean isOverLiquid()
	{
		boolean foundLiquid = false;
		boolean foundSolid = false;
		
		// check collision boxes below player
		ArrayList<Box> blockCollisions = MC.world
			.getBlockCollisions(MC.player,
				MC.player.getBoundingBox().offset(0, -0.5, 0))
			.map(VoxelShape::getBoundingBox)
			.collect(Collectors.toCollection(() -> new ArrayList<>()));
		
		for(Box bb : blockCollisions)
		{
			BlockPos pos = new BlockPos(bb.getCenter());
			Material material = BlockUtils.getState(pos).getMaterial();
			
			if(material == Material.WATER || material == Material.LAVA)
				foundLiquid = true;
			else if(material != Material.AIR)
				foundSolid = true;
		}
		
		return foundLiquid && !foundSolid;
	}
	
	public boolean shouldBeSolid()
	{
		return isEnabled() && MC.player != null && MC.player.fallDistance <= 3
			&& !MC.options.keySneak.isPressed() && !MC.player.isTouchingWater();
	}
}
