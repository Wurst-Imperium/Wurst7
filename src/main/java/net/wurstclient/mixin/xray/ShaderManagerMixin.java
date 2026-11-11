/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin.xray;

import java.nio.charset.StandardCharsets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.google.common.hash.Hashing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;

@Mixin(ShaderManager.class)
public class ShaderManagerMixin
{
	/**
	 * Checks if terrain.vsh still matches the 26.4-snapshot-8 code, then
	 * modifies it to use the alpha value saved by SectionBufferBuilderMixin
	 * instead of the hardcoded 1.0f.
	 *
	 * This is obviously very fragile and should be replaced as soon as a future
	 * snapshot brings back an easier way to modify terrain opacity.
	 */
	@ModifyExpressionValue(method = "loadShader",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/packs/resources/Resource;readAllAsString()Ljava/lang/String;"),
		allow = 1)
	private static String readTerrainOpacity(String source, Identifier location)
	{
		if(!location.equals(
			Identifier.withDefaultNamespace("shaders/core/terrain.vsh")))
			return source;
		
		String expectedHash =
			"cbfce26480627d0ecccaa7072c985377610fc0eb45b9c567f6c1a003832cd78d";
		String actualHash = Hashing.sha256()
			.hashString(source.replace("\r\n", "\n"), StandardCharsets.UTF_8)
			.toString();
		if(!actualHash.equals(expectedHash))
			throw new IllegalStateException("Wurst Opacity X-Ray: " + location
				+ " has changed (SHA-256 " + actualHash
				+ "). Review ShaderManagerMixin and SectionBufferBuilderMixin"
				+ " before updating the expected shader hash.");
		
		return source.replace("return VertexData(position, color, UV0, UV2);",
			"color.a = float(texelFetch(vertexBuffer, startOffset + 1).g) / 255.0;\n"
				+ "    return VertexData(position, color, UV0, UV2);");
	}
}
