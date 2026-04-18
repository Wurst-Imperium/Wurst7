/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.loader.api.FabricLoader;
import net.wurstclient.mixin.xray.sodium.BlockRendererMixin;

public final class WurstMixinPlugin implements IMixinConfigPlugin
{
	@Override
	public boolean shouldApplyMixin(String targetClassName,
		String mixinClassName)
	{
		if(BlockRendererMixin.class.getName().equals(mixinClassName)
			&& FabricLoader.getInstance().isModLoaded("betterblockentities"))
			return false;
		
		return true;
	}
	
	@Override
	public void onLoad(String mixinPackage)
	{}
	
	@Override
	public String getRefMapperConfig()
	{
		return null;
	}
	
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
	{}
	
	@Override
	public List<String> getMixins()
	{
		return null;
	}
	
	@Override
	public void preApply(String targetClassName, ClassNode targetClass,
		String mixinClassName, IMixinInfo mixinInfo)
	{}
	
	@Override
	public void postApply(String targetClassName, ClassNode targetClass,
		String mixinClassName, IMixinInfo mixinInfo)
	{}
}
