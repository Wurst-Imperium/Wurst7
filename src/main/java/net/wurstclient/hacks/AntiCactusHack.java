/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PreMotionListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"NoCactus", "anti cactus", "no cactus"})
public final class AntiCactusHack extends Hack implements PreMotionListener
{
	private static final VoxelShape VANILLA_HEIGHT_CACTUS_SHAPE =
		Block.column(16, 0, 15);
	
	private final CheckboxSetting preventCactusTopDamage = new CheckboxSetting(
		"Prevent cactus-top damage",
		"description.wurst.setting.anticactus.prevent_cactus-top_damage", true);
	
	public AntiCactusHack()
	{
		super("AntiCactus");
		setCategory(Category.BLOCKS);
		addSetting(preventCactusTopDamage);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PreMotionListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PreMotionListener.class, this);
	}
	
	public VoxelShape getCollisionShape()
	{
		return preventCactusTopDamage.isChecked() ? Shapes.block()
			: VANILLA_HEIGHT_CACTUS_SHAPE;
	}
	
	@Override
	public void onPreMotion()
	{
		if(!preventCactusTopDamage.isChecked()
			|| MC.player.getDeltaMovement().y > 0 || !isAboveCactus())
			return;
		
		Vec3 pos = MC.player.position();
		boolean hCollision = MC.player.horizontalCollision;
		ClientPacketListener connection = MC.player.connection;
		
		connection.send(new Pos(pos, true, hCollision));
		connection.send(new Pos(pos.add(0, 1e-6, 0), false, hCollision));
		connection.send(new Pos(pos, true, hCollision));
	}
	
	private boolean isAboveCactus()
	{
		AABB below = MC.player.getBoundingBox().move(0, -2, 0);
		BlockPos min = BlockPos.containing(below.minX, below.minY, below.minZ);
		BlockPos max =
			BlockPos.containing(below.maxX, MC.player.getY(), below.maxZ);
		
		for(BlockPos pos : BlockPos.betweenClosed(min, max))
			if(MC.level.getBlockState(pos).is(Blocks.CACTUS)
				&& MC.player.getY() >= pos.getY() + 1)
				return true;
			
		return false;
	}
}
