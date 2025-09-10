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

import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(LabelCommandRenderer.class_12050.class)
public class LabelCommandRendererMixin
{
	// Note: These fields are backwards compared to 25w36b, might be a bug.
	@Shadow // labelCommands
	private List<OrderedRenderCommandQueueImpl.LabelCommand> field_62987;
	
	@Shadow // seeThroughLabelCommands
	private List<OrderedRenderCommandQueueImpl.LabelCommand> field_62988;
	
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"),
		method = "method_74829(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private void wrapLabelScale(MatrixStack matrices, float x, float y, float z,
		Operation<Void> original, MatrixStack matrices2, @Nullable Vec3d vec3d,
		Text text, boolean bl, int i, double d)
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
		
		matrices.scale(scale, -scale, scale);
	}
	
	/**
	 * Modifies the notSneaking parameter to force labels to show when NameTags
	 * is enabled.
	 */
	@ModifyVariable(at = @At("HEAD"),
		method = "method_74829(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V",
		argsOnly = true)
	private boolean forceNotSneaking(boolean notSneaking)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		return nameTags.isEnabled() || notSneaking;
	}
	
	/**
	 * Swaps the target list for the first add() call
	 * (field_62988 -> field_62987) if NameTags is enabled in
	 * see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 0),
		method = "method_74829(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<OrderedRenderCommandQueueImpl.LabelCommand> swapFirstList(
		List<OrderedRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			// field_62988 is seeThroughLabelCommands,
			// field_62987 is labelCommands
			// Swap the first add() call from
			// seeThroughLabelCommands to labelCommands
			if(originalList == field_62988)
				return field_62987;
			
		return originalList;
	}
	
	/**
	 * Swaps the target list for the second add() call
	 * (field_62987 -> field_62988) if NameTags is enabled in
	 * see-through mode.
	 */
	@ModifyReceiver(
		at = @At(value = "INVOKE",
			target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
			ordinal = 1),
		method = "method_74829(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/text/Text;ZID)V")
	private List<OrderedRenderCommandQueueImpl.LabelCommand> swapSecondList(
		List<OrderedRenderCommandQueueImpl.LabelCommand> originalList,
		Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			// field_62987 is labelCommands,
			// field_62988 is seeThroughLabelCommands
			// Swap the second add() call from labelCommands to
			// seeThroughLabelCommands
			if(originalList == field_62987)
				return field_62988;
			
		return originalList;
	}
}
