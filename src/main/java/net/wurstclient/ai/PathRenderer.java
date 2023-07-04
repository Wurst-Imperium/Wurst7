/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
	public static void renderArrow(BlockPos start, BlockPos end, int regionX,
		int regionZ)
	{
		int startX = start.getX() - regionX;
		int startY = start.getY();
		int startZ = start.getZ() - regionZ;
		
		int endX = end.getX() - regionX;
		int endY = end.getY();
		int endZ = end.getZ() - regionZ;
		
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
	
	public static void renderNode(BlockPos pos, int regionX, int regionZ)
	{
		glPushMatrix();
		
		glTranslated(pos.getX() - regionX, pos.getY(), pos.getZ() - regionZ);
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
