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
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.entity.command.EntityRenderCommandQueue;
import net.minecraft.client.render.entity.command.EntityRenderCommandQueueImpl;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(EntityRenderCommandQueueImpl.class)
public abstract class EntityRenderCommandQueueImplMixin
	implements EntityRenderCommandQueue
{
	@Shadow
	@Final
	private List<EntityRenderCommandQueueImpl.LabelCommand> seeThroughLabelCommands;
	
	@Shadow
	@Final
	private List<EntityRenderCommandQueueImpl.LabelCommand> labelCommands;
	
	/**
	 * Intercepts the matrices.scale() call in pushLabel to apply NameTags scale
	 * adjustments.
	 */
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"),
		method = "pushLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
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
		method = "pushLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V",
		argsOnly = true)
	private boolean forceNotSneaking(boolean notSneaking)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		return nameTags.isEnabled() || notSneaking;
	}
	
	/**
	 * Swaps the target list for the first add() call
	 * (labelCommands -> seeThroughLabelCommands)
	 * if NameTags is enabled in see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 0),
		method = "pushLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<EntityRenderCommandQueueImpl.LabelCommand> swapFirstList(
		List<EntityRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough()
			&& originalList == labelCommands)
			return seeThroughLabelCommands;
		
		return originalList;
	}
	
	/**
	 * Swaps the target list for the second add() call
	 * (seeThroughLabelCommands -> labelCommands)
	 * if NameTags is enabled in see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 1),
		method = "pushLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<EntityRenderCommandQueueImpl.LabelCommand> swapSecondList(
		List<EntityRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough()
			&& originalList == seeThroughLabelCommands)
			return labelCommands;
		
		return originalList;
	}
}
