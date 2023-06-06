/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.chestesp;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public final class ChestEspRenderer
{
	private static int solidBox;
	private static int outlinedBox;
	
	private final int regionX;
	private final int regionZ;
	private final Vec3d start;
	
	public ChestEspRenderer()
	{
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		regionX = (camPos.getX() >> 9) * 512;
		regionZ = (camPos.getZ() >> 9) * 512;
		
		start = RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos())
			.subtract(regionX, 0, regionZ);
	}
	
	public void renderBoxes(ChestEspGroup group)
	{
		float[] colorF = group.getColorF();
		
		for(Box box : group.getBoxes())
		{
			GL11.glPushMatrix();
			
			GL11.glTranslated(box.minX - regionX, box.minY, box.minZ - regionZ);
			
			GL11.glScaled(box.maxX - box.minX, box.maxY - box.minY,
				box.maxZ - box.minZ);
			
			GL11.glColor4f(colorF[0], colorF[1], colorF[2], 0.25F);
			GL11.glCallList(solidBox);
			
			GL11.glColor4f(colorF[0], colorF[1], colorF[2], 0.5F);
			GL11.glCallList(outlinedBox);
			
			GL11.glPopMatrix();
		}
	}
	
	public void renderLines(ChestEspGroup group)
	{
		float[] colorF = group.getColorF();
		GL11.glColor4f(colorF[0], colorF[1], colorF[2], 0.5F);
		
		GL11.glBegin(GL11.GL_LINES);
		
		for(Box box : group.getBoxes())
		{
			Vec3d end = box.getCenter().subtract(regionX, 0, regionZ);
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
		
		GL11.glEnd();
	}
	
	public static void prepareBuffers()
	{
		closeBuffers();
		Box box = new Box(BlockPos.ORIGIN);
		
		solidBox = GL11.glGenLists(1);
		GL11.glNewList(solidBox, GL11.GL_COMPILE);
		RenderUtils.drawSolidBox(box);
		GL11.glEndList();
		
		outlinedBox = GL11.glGenLists(1);
		GL11.glNewList(outlinedBox, GL11.GL_COMPILE);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
	}
	
	public static void closeBuffers()
	{
		if(solidBox != 0)
		{
			GL11.glDeleteLists(solidBox, 1);
			solidBox = 0;
		}
		
		if(outlinedBox != 0)
		{
			GL11.glDeleteLists(outlinedBox, 1);
			outlinedBox = 0;
		}
	}
}
