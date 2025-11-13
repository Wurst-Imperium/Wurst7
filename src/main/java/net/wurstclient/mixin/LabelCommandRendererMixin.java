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
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.NameTagsHack;

@Mixin(NameTagFeatureRenderer.Storage.class)
public class LabelCommandRendererMixin
{
	@Shadow
	@Final
	List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsSeethrough;
	
	@Shadow
	@Final
	List<SubmitNodeStorage.NameTagSubmit> nameTagSubmitsNormal;
	
	@WrapOperation(
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"),
		method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V")
	private void wrapLabelScale(PoseStack matrices, float x, float y, float z,
		Operation<Void> original, PoseStack matrices2, @Nullable Vec3 vec3d,
		int i, Component text, boolean bl, int j, double d,
		CameraRenderState state)
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
		method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V",
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
		method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V")
	private List<SubmitNodeStorage.NameTagSubmit> swapFirstList(
		List<SubmitNodeStorage.NameTagSubmit> originalList, Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			if(originalList == nameTagSubmitsNormal)
				return nameTagSubmitsSeethrough;
			
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
		method = "add(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V")
	private List<SubmitNodeStorage.NameTagSubmit> swapSecondList(
		List<SubmitNodeStorage.NameTagSubmit> originalList, Object labelCommand)
	{
		NameTagsHack nameTags = WurstClient.INSTANCE.getHax().nameTagsHack;
		
		if(nameTags.isEnabled() && nameTags.isSeeThrough())
			if(originalList == nameTagSubmitsSeethrough)
				return nameTagSubmitsNormal;
			
		return originalList;
	}
}
