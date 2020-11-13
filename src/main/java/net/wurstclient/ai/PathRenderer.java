/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.util.math.BlockPos;

public final class PathRenderer
{
	public static void renderArrow(BlockPos start, BlockPos end)
	{
		int startX = start.getX();
		int startY = start.getY();
		int startZ = start.getZ();
		
		int endX = end.getX();
		int endY = end.getY();
		int endZ = end.getZ();
		
		glPushMatrix();
		
		glBegin(GL_LINES);
		{
			glVertex3d(startX, startY, startZ);
			glVertex3d(endX, endY, endZ);
		}
		glEnd();
		
		glTranslated(endX, endY, endZ);
		double scale = 1 / 16D;
		glScaled(scale, scale, scale);
		
		glRotated(Math.toDegrees(Math.atan2(endY - startY, startZ - endZ)) + 90,
			1, 0, 0);
		glRotated(
			Math.toDegrees(Math.atan2(endX - startX,
				Math.sqrt(
					Math.pow(endY - startY, 2) + Math.pow(endZ - startZ, 2)))),
			0, 0, 1);
		
		glBegin(GL_LINES);
		{
			glVertex3d(0, 2, 1);
			glVertex3d(-1, 2, 0);
			
			glVertex3d(-1, 2, 0);
			glVertex3d(0, 2, -1);
			
			glVertex3d(0, 2, -1);
			glVertex3d(1, 2, 0);
			
			glVertex3d(1, 2, 0);
			glVertex3d(0, 2, 1);
			
			glVertex3d(1, 2, 0);
			glVertex3d(-1, 2, 0);
			
			glVertex3d(0, 2, 1);
			glVertex3d(0, 2, -1);
			
			glVertex3d(0, 0, 0);
			glVertex3d(1, 2, 0);
			
			glVertex3d(0, 0, 0);
			glVertex3d(-1, 2, 0);
			
			glVertex3d(0, 0, 0);
			glVertex3d(0, 2, -1);
			
			glVertex3d(0, 0, 0);
			glVertex3d(0, 2, 1);
		}
		glEnd();
		
		glPopMatrix();
	}
	
	public static void renderNode(BlockPos pos)
	{
		glPushMatrix();
		
		glTranslated(pos.getX(), pos.getY(), pos.getZ());
		glScaled(0.1, 0.1, 0.1);
		
		glBegin(GL_LINES);
		{
			// middle part
			glVertex3d(0, 0, 1);
			glVertex3d(-1, 0, 0);
			
			glVertex3d(-1, 0, 0);
			glVertex3d(0, 0, -1);
			
			glVertex3d(0, 0, -1);
			glVertex3d(1, 0, 0);
			
			glVertex3d(1, 0, 0);
			glVertex3d(0, 0, 1);
			
			// top part
			glVertex3d(0, 1, 0);
			glVertex3d(1, 0, 0);
			
			glVertex3d(0, 1, 0);
			glVertex3d(-1, 0, 0);
			
			glVertex3d(0, 1, 0);
			glVertex3d(0, 0, -1);
			
			glVertex3d(0, 1, 0);
			glVertex3d(0, 0, 1);
			
			// bottom part
			glVertex3d(0, -1, 0);
			glVertex3d(1, 0, 0);
			
			glVertex3d(0, -1, 0);
			glVertex3d(-1, 0, 0);
			
			glVertex3d(0, -1, 0);
			glVertex3d(0, 0, -1);
			
			glVertex3d(0, -1, 0);
			glVertex3d(0, 0, 1);
		}
		glEnd();
		
		glPopMatrix();
	}
}
