/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CactusBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.wurstclient.WurstClient;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.CactusCollisionShapeListener.CactusCollisionShapeEvent;

@Mixin(CactusBlock.class)
public abstract class CactusBlockMixin extends Block
{
	private CactusBlockMixin(WurstClient wurst, Settings block$Settings_1)
	{
		super(block$Settings_1);
	}
	
	@Inject(at = {@At("HEAD")},
		method = {
			"getOutlineShape(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;"},
		cancellable = true)
	private void onGetCollisionShape(BlockState blockState_1,
		BlockView blockView_1, BlockPos blockPos_1,
		ShapeContext entityContext_1, CallbackInfoReturnable<VoxelShape> cir)
	{
		CactusCollisionShapeEvent event = new CactusCollisionShapeEvent();
		EventManager.fire(event);
		
		VoxelShape collisionShape = event.getCollisionShape();
		if(collisionShape != null)
			cir.setReturnValue(collisionShape);
	}
}
