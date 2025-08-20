/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.entity.command.BatchingEntityRenderCommandQueue;
import net.minecraft.client.render.entity.command.EntityRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(net.minecraft.class_11788.class)
public abstract class EntityRenderCommandQueueImplMixin
	implements EntityRenderCommandQueue
{
	@Shadow
	private List<BatchingEntityRenderCommandQueue.LabelCommand> field_62226;
	
	@Shadow
	private List<BatchingEntityRenderCommandQueue.LabelCommand> field_62227;
	
	/**
	 * Intercepts the matrices.scale() call in method_73482 to apply NameTags
	 * scale adjustments.
	 */
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"),
		method = "method_73482(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private void wrapLabelScale(MatrixStack matrices, float x, float y, float z,
		Operation<Void> original, MatrixStack matrices2, @Nullable Vec3d pos,
		Text label, boolean notSneaking, int light,
		double squaredDistanceToCamera)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		if(!nameTags.isEnabled())
		{
			original.call(matrices, x, y, z);
			return;
		}
		
		float scale = 0.025F * nameTags.getScale();
		double distance = Math.sqrt(squaredDistanceToCamera);
		if(distance > 10)
			scale *= distance / 10;
		
		matrices.scale(scale, -scale, scale);
	}
	
	/**
	 * Modifies the notSneaking parameter to force labels to show when NameTags
	 * is enabled.
	 */
	@ModifyVariable(at = @At("HEAD"),
		method = "method_73482(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V",
		argsOnly = true)
	private boolean forceNotSneaking(boolean notSneaking)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		return nameTags.isEnabled() || notSneaking;
	}
	
	/**
	 * Swaps the target list for the first add() call
	 * (field_62227 -> field_62226) if NameTags is enabled in see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 0),
		method = "method_73482(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<BatchingEntityRenderCommandQueue.LabelCommand> swapFirstList(
		List<BatchingEntityRenderCommandQueue.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			// field_62227 = see-through labels, field_62226 = regular labels
			if(originalList == field_62227) // see-through labels
				return field_62226; // return regular labels
				
		return originalList;
	}
	
	/**
	 * Swaps the target list for the second add() call
	 * (field_62226 -> field_62227) if NameTags is enabled in see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 1),
		method = "method_73482(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<BatchingEntityRenderCommandQueue.LabelCommand> swapSecondList(
		List<BatchingEntityRenderCommandQueue.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			// field_62226 = regular labels, field_62227 = see-through labels
			if(originalList == field_62226) // regular labels
				return field_62227; // return see-through labels
				
		return originalList;
	}
}
