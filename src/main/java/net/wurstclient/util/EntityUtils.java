/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.registry.Registry;

import java.util.Optional;

public enum EntityUtils
{
	;
	
	public static EntityType<?> getEntityTypeFromName(String name)
	{
		try
		{
			return Registry.ENTITY_TYPE.getOrEmpty(new Identifier(name)).orElse(null);
			
		}catch(InvalidIdentifierException e)
		{
			return null;
		}
	}
	
	public static String getEntityName(EntityType<?> entityType)
	{
		return Registry.ENTITY_TYPE.getId(entityType).toString();
	}
	
}