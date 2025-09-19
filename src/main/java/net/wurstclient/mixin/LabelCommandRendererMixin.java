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

import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(LabelCommandRenderer.Commands.class)
public class LabelCommandRendererMixin
{
	@Shadow
	@Final
	List<OrderedRenderCommandQueueImpl.LabelCommand> seethroughLabels;
	
	@Shadow
	@Final
	List<OrderedRenderCommandQueueImpl.LabelCommand> normalLabels;
	
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"),
		method = "add(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V")
	private void wrapLabelScale(MatrixStack matrices, float x, float y, float z,
		Operation<Void> original, MatrixStack matrices2, @Nullable Vec3d vec3d,
		int i, Text text, boolean bl, int j, double d, CameraRenderState state)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		if(!nameTags.isEnabled())
		{
			original.call(matrices, x, y, z);
			return;
		}
		
		float scale = 0.025F * nameTags.getScale();
		double distance = Math.sqrt(d);
		if(distance > 10)
			scale *= distance / 10;
		
		original.call(matrices, scale, -scale, scale);
	}
	
	/**
	 * Modifies the notSneaking parameter to force labels to show when NameTags
	 * is enabled.
	 */
	@ModifyVariable(at = @At("HEAD"),
		method = "add(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V",
		argsOnly = true)
	private boolean forceNotSneaking(boolean notSneaking)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		return nameTags.isEnabled() || notSneaking;
	}
	
	/**
	 * Swaps the target list for the first add() call
	 * (normalLabels -> seethroughLabels) if NameTags is enabled in
	 * see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 0),
		method = "add(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V")
	private List<OrderedRenderCommandQueueImpl.LabelCommand> swapFirstList(
		List<OrderedRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			if(originalList == normalLabels)
				return seethroughLabels;
			
		return originalList;
	}
	
	/**
	 * Swaps the target list for the second add() call
	 * (seethroughLabels -> normalLabels) if NameTags is enabled in
	 * see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 1),
		method = "add(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V")
	private List<OrderedRenderCommandQueueImpl.LabelCommand> swapSecondList(
		List<OrderedRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			if(originalList == seethroughLabels)
				return normalLabels;
			
		return originalList;
	}
}
