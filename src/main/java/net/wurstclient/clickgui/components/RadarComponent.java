/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterAnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.clickgui.ClickGui;
import net.wurstclient.clickgui.ClickGuiIcons;
import net.wurstclient.clickgui.Component;
import net.wurstclient.hacks.RadarHack;
import net.wurstclient.util.EntityUtils;
import net.wurstclient.util.RenderUtils;

public final class RadarComponent extends Component
{
	private final RadarHack hack;
	
	public RadarComponent(RadarHack hack)
	{
		this.hack = hack;
		setWidth(getDefaultWidth());
		setHeight(getDefaultHeight());
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY,
		float partialTicks)
	{
		// Can't make this a field because RadarComponent is initialized earlier
		// than ClickGui.
		ClickGui gui = WURST.getGui();
		
		int x1 = getX();
		int x2 = x1 + getWidth();
		int y1 = getY();
		int y2 = y1 + getHeight();
		float middleX = (x1 + x2) / 2F;
		float middleY = (y1 + y2) / 2F;
		
		// tooltip
		if(isHovering(mouseX, mouseY))
			gui.setTooltip("");
		
		// background
		context.fill(x1, y1, x2, y2,
			RenderUtils.toIntColor(gui.getBgColor(), gui.getOpacity()));
		
		Matrix3x2fStack matrixStack = context.getMatrices();
		matrixStack.pushMatrix();
		matrixStack.translate(middleX, middleY);
		
		ClientPlayerEntity player = MC.player;
		if(!hack.isRotateEnabled())
			matrixStack.rotate(
				(180 + player.getYaw()) * MathHelper.RADIANS_PER_DEGREE);
		
		// arrow
		ClickGuiIcons.drawRadarArrow(context, -2, -2, 2, 2);
		
		matrixStack.popMatrix();
		Vec3d lerpedPlayerPos = EntityUtils.getLerpedPos(player, partialTicks);
		
		// points
		for(Entity e : hack.getEntities())
		{
			Vec3d lerpedEntityPos = EntityUtils.getLerpedPos(e, partialTicks);
			double diffX = lerpedEntityPos.x - lerpedPlayerPos.x;
			double diffZ = lerpedEntityPos.z - lerpedPlayerPos.z;
			double distance = Math.sqrt(diffX * diffX + diffZ * diffZ)
				* (getWidth() * 0.5 / hack.getRadius());
			double neededRotation = Math.toDegrees(Math.atan2(diffZ, diffX));
			double angle;
			if(hack.isRotateEnabled())
				angle = Math.toRadians(player.getYaw() - neededRotation - 90);
			else
				angle = Math.toRadians(180 - neededRotation - 90);
			double renderX = Math.sin(angle) * distance;
			double renderY = Math.cos(angle) * distance;
			
			if(Math.abs(renderX) > getWidth() / 2.0
				|| Math.abs(renderY) > getHeight() / 2.0)
				continue;
			
			float ex1 = middleX + (float)renderX - 0.5F;
			float ex2 = middleX + (float)renderX + 0.5F;
			float ey1 = middleY + (float)renderY - 0.5F;
			float ey2 = middleY + (float)renderY + 0.5F;
			RenderUtils.fill2D(context, ex1, ey1, ex2, ey2, getEntityColor(e));
		}
	}
	
	private int getEntityColor(Entity e)
	{
		if(WURST.getFriends().isFriend(e))
			return 0xFF0000FF;
		if(e instanceof PlayerEntity)
			return 0xFFFF0000;
		if(e instanceof Monster)
			return 0xFFFF8000;
		if(e instanceof AnimalEntity || e instanceof AmbientEntity
			|| e instanceof WaterCreatureEntity
			|| e instanceof WaterAnimalEntity)
			return 0xFF00FF00;
		return 0xFF808080;
	}
	
	@Override
	public int getDefaultWidth()
	{
		return 96;
	}
	
	@Override
	public int getDefaultHeight()
	{
		return 96;
	}
}
