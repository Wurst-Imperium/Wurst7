/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.SectionBufferBuilder;

import net.minecraft.util.ARGB;

@Mixin(SectionBufferBuilder.class)
public class SectionBufferBuilderMixin
{
	@Unique
	private static final int OPACITY_DUMMY = 0xA5A5A5A5;
	
	/**
	 * Checks that vertex buffer face size is still 32, then places a dummy
	 * value in the sixth word of each 32-byte terrain face before vanilla can
	 * write to it.
	 */
	@WrapOperation(
		method = "addQuad(Lnet/minecraft/client/resources/model/geometry/BakedQuad;FFFIIJ)V",
		at = @At(value = "INVOKE",
			target = "Lcom/mojang/blaze3d/vertex/ByteBufferBuilder;reserve(I)J"),
		allow = 1)
	private long reserveOpacityWord(ByteBufferBuilder buffer, int size,
		Operation<Long> original)
	{
		if(size != 32)
			throw new IllegalStateException("Wurst Opacity X-Ray: expected a"
				+ " 32-byte terrain face, got " + size
				+ ". Review SectionBufferBuilderMixin and ShaderManagerMixin.");
		
		long pointer = original.call(buffer, size);
		MemoryUtil.memPutInt(pointer + 20, OPACITY_DUMMY);
		return pointer;
	}
	
	/**
	 * Checks if the dummy value is still there (meaning the sixth word is still
	 * unused in vanilla), then replaces it with the alpha value of the face.
	 */
	@Inject(
		method = "addQuad(Lnet/minecraft/client/resources/model/geometry/BakedQuad;FFFIIJ)V",
		at = @At("RETURN"),
		allow = 1)
	private void storeOpacity(CallbackInfo ci,
		@Local(argsOnly = true, ordinal = 0) int color,
		@Local(ordinal = 1) long pointer)
	{
		if(MemoryUtil.memGetInt(pointer + 20) != OPACITY_DUMMY)
			throw new IllegalStateException("Wurst Opacity X-Ray: terrain face"
				+ " bytes 20-23 are no longer unused. Review"
				+ " SectionBufferBuilderMixin and ShaderManagerMixin.");
		
		MemoryUtil.memPutInt(pointer + 20, ARGB.alpha(color));
	}
}
