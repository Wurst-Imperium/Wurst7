/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public enum BBEModCompat
{
	;
	
	private static final VersionPredicate BROKEN_BBE_VERSIONS;
	static
	{
		try
		{
			BROKEN_BBE_VERSIONS = VersionPredicate.parse(">=1.3.2 <1.3.4");
			
		}catch(VersionParsingException e)
		{
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public static boolean isBrokenBBEInstalled()
	{
		return FabricLoader.getInstance().getModContainer("betterblockentities")
			.map(ModContainer::getMetadata).map(ModMetadata::getVersion)
			.map(BROKEN_BBE_VERSIONS::test).orElse(false);
	}
}
