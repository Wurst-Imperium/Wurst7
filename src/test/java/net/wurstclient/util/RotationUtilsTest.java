/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RotationUtilsTest
{
	// Most of the other methods here depend on MC.player,
	// making them very hard to write tests for.
	
	@Test
	void testLimitAngleChangeWithMax()
	{
		float result = RotationUtils.limitAngleChange(0, 179, 90);
		assertEquals(90, result);
		
		result = RotationUtils.limitAngleChange(0, -179, 90);
		assertEquals(-90, result);
		
		result = RotationUtils.limitAngleChange(179, -179, 90);
		assertEquals(181, result);
		
		result = RotationUtils.limitAngleChange(-179, 179, 90);
		assertEquals(-181, result);
	}
	
	@Test
	void testLimitAngleChangeWithoutMax()
	{
		float result = RotationUtils.limitAngleChange(0, 179);
		assertEquals(179, result);
		
		result = RotationUtils.limitAngleChange(0, -179);
		assertEquals(-179, result);
		
		result = RotationUtils.limitAngleChange(179, -179);
		assertEquals(181, result);
		
		result = RotationUtils.limitAngleChange(-179, 179);
		assertEquals(-181, result);
	}
}
