/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.clickgui.components;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
	public void render(GuiGraphics context, int mouseX, int mouseY,
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
		
		PoseStack matrixStack = context.pose();
		matrixStack.pushPose();
		matrixStack.translate(middleX, middleY, 0);
		
		LocalPlayer player = MC.player;
		if(!hack.isRotateEnabled())
			matrixStack.mulPose(new Quaternionf()
				.rotationZ((180 + player.getYRot()) * Mth.DEG_TO_RAD));
		
		// arrow
		ClickGuiIcons.drawRadarArrow(context, -2, -2, 2, 2);
		
		matrixStack.popPose();
		Vec3 lerpedPlayerPos = EntityUtils.getLerpedPos(player, partialTicks);
		
		// points
		for(Entity e : hack.getEntities())
		{
			Vec3 lerpedEntityPos = EntityUtils.getLerpedPos(e, partialTicks);
			double diffX = lerpedEntityPos.x - lerpedPlayerPos.x;
			double diffZ = lerpedEntityPos.z - lerpedPlayerPos.z;
			double distance = Math.sqrt(diffX * diffX + diffZ * diffZ)
				* (getWidth() * 0.5 / hack.getRadius());
			double neededRotation = Math.toDegrees(Math.atan2(diffZ, diffX));
			double angle;
			if(hack.isRotateEnabled())
				angle = Math.toRadians(player.getYRot() - neededRotation - 90);
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
		if(e instanceof Player)
			return 0xFFFF0000;
		if(e instanceof Enemy)
			return 0xFFFF8000;
		if(e instanceof Animal || e instanceof AmbientCreature
			|| e instanceof WaterAnimal)
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
