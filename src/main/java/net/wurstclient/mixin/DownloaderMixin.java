/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.mixin;

import java.nio.file.Path;
import java.util.UUID;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.session.Session;
import net.minecraft.util.Downloader;
import net.minecraft.util.Uuids;
import net.wurstclient.WurstClient;

@Mixin(Downloader.class)
public abstract class DownloaderMixin implements AutoCloseable
{
	@Shadow
	@Final
	private Path directory;
	
	/**
	 * Patches a fingerprinting vulnerability by creating a separate cache
	 * folder for each Minecraft account.
	 *
	 * <p>
	 * This mixin targets the <code>entries.forEach()</code> lambda in
	 * <code>download(Config, Map)</code>.
	 *
	 * @see https://github.com/Wurst-Imperium/Wurst7/issues/1226
	 */
	@WrapOperation(at = @At(value = "INVOKE",
		target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;",
		ordinal = 0,
		remap = false), method = "method_55485", remap = false)
	private Path wrapResolve(Path instance, String filename,
		Operation<Path> original)
	{
		Path result = original.call(instance, filename);
		
		// If the path has already been modified by another mod (likely trying
		// to patch the same exploit), don't modify it further.
		if(result == null || !result.getParent().equals(directory))
			return result;
			
		// "getUuidOrNull" seems to be an outdated Yarn name, as Minecraft
		// 1.21.10 treats this like a non-null method. Just in case, we manually
		// fallback to the offline UUID if it ever does return null.
		Session session = WurstClient.MC.getSession();
		UUID uuid = session.getUuidOrNull();
		if(uuid == null)
			uuid = Uuids.getOfflinePlayerUuid(session.getUsername());
		
		return result.getParent().resolve(uuid.toString())
			.resolve(result.getFileName());
	}
}
