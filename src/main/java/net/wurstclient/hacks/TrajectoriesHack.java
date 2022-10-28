/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.*;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"ArrowTrajectories", "ArrowPrediction", "aim assist",
	"arrow trajectories"})
public final class TrajectoriesHack extends Hack implements RenderListener
{
	private final ColorSetting color =
		new ColorSetting("Color", "Color of the trajectory.", Color.GREEN);
	
	public TrajectoriesHack()
	{
		super("Trajectories");
		setCategory(Category.RENDER);
		addSetting(color);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		matrixStack.push();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		
		RenderUtils.applyCameraRotationOnly();
		
		ArrayList<Vec3d> path = getPath(partialTicks);
		Vec3d camPos = RenderUtils.getCameraPos();
		
		drawLine(matrixStack, path, camPos);
		
		if(!path.isEmpty())
		{
			Vec3d end = path.get(path.size() - 1);
			drawEndOfLine(matrixStack, end, camPos);
		}
		
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		matrixStack.pop();
	}
	
	private void drawLine(MatrixStack matrixStack, ArrayList<Vec3d> path,
		Vec3d camPos)
	{
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP,
			VertexFormats.POSITION);
		float[] colorF = color.getColorF();
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.75F);
		
		for(Vec3d point : path)
			bufferBuilder
				.vertex(matrix, (float)(point.x - camPos.x),
					(float)(point.y - camPos.y), (float)(point.z - camPos.z))
				.next();
		
		tessellator.draw();
	}
	
	private void drawEndOfLine(MatrixStack matrixStack, Vec3d end, Vec3d camPos)
	{
		double renderX = end.x - camPos.x;
		double renderY = end.y - camPos.y;
		double renderZ = end.z - camPos.z;
		float[] colorF = color.getColorF();
		
		matrixStack.push();
		matrixStack.translate(renderX - 0.5, renderY - 0.5, renderZ - 0.5);
		
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.25F);
		RenderUtils.drawSolidBox(matrixStack);
		
		RenderSystem.setShaderColor(colorF[0], colorF[1], colorF[2], 0.75F);
		RenderUtils.drawOutlinedBox(matrixStack);
		
		matrixStack.pop();
	}
	
	private ArrayList<Vec3d> getPath(float partialTicks)
	{
		ClientPlayerEntity player = MC.player;
		ArrayList<Vec3d> path = new ArrayList<>();
		
		ItemStack stack = player.getMainHandStack();
		Item item = stack.getItem();
		
		// check if item is throwable
		if(stack.isEmpty() || !isThrowable(item))
			return path;
		
		// calculate starting position
		double arrowPosX = player.lastRenderX
			+ (player.getX() - player.lastRenderX) * partialTicks
			- Math.cos(Math.toRadians(player.getYaw())) * 0.16;
		
		double arrowPosY = player.lastRenderY
			+ (player.getY() - player.lastRenderY) * partialTicks
			+ player.getStandingEyeHeight() - 0.1;
		
		double arrowPosZ = player.lastRenderZ
			+ (player.getZ() - player.lastRenderZ) * partialTicks
			- Math.sin(Math.toRadians(player.getYaw())) * 0.16;
		
		// Motion factor. Arrows go faster than snowballs and all that...
		double arrowMotionFactor = item instanceof RangedWeaponItem ? 1.0 : 0.4;
		
		double yaw = Math.toRadians(player.getYaw());
		double pitch = Math.toRadians(player.getPitch());
		
		// calculate starting motion
		double arrowMotionX =
			-Math.sin(yaw) * Math.cos(pitch) * arrowMotionFactor;
		double arrowMotionY = -Math.sin(pitch) * arrowMotionFactor;
		double arrowMotionZ =
			Math.cos(yaw) * Math.cos(pitch) * arrowMotionFactor;
		
		// 3D Pythagorean theorem. Returns the length of the arrowMotion vector.
		double arrowMotion = Math.sqrt(arrowMotionX * arrowMotionX
			+ arrowMotionY * arrowMotionY + arrowMotionZ * arrowMotionZ);
		
		arrowMotionX /= arrowMotion;
		arrowMotionY /= arrowMotion;
		arrowMotionZ /= arrowMotion;
		
		// apply bow charge
		if(item instanceof RangedWeaponItem)
		{
			float bowPower = (72000 - player.getItemUseTimeLeft()) / 20.0f;
			bowPower = (bowPower * bowPower + bowPower * 2.0f) / 3.0f;
			
			if(bowPower > 1 || bowPower <= 0.1F)
				bowPower = 1;
			
			bowPower *= 3F;
			arrowMotionX *= bowPower;
			arrowMotionY *= bowPower;
			arrowMotionZ *= bowPower;
			
		}else
		{
			arrowMotionX *= 1.5;
			arrowMotionY *= 1.5;
			arrowMotionZ *= 1.5;
		}
		
		double gravity = getProjectileGravity(item);
		Vec3d eyesPos = RotationUtils.getEyesPos();
		
		for(int i = 0; i < 1000; i++)
		{
			// add to path
			Vec3d arrowPos = new Vec3d(arrowPosX, arrowPosY, arrowPosZ);
			path.add(arrowPos);
			
			// apply motion
			arrowPosX += arrowMotionX * 0.1;
			arrowPosY += arrowMotionY * 0.1;
			arrowPosZ += arrowMotionZ * 0.1;
			
			// apply air friction
			arrowMotionX *= 0.999;
			arrowMotionY *= 0.999;
			arrowMotionZ *= 0.999;
			
			// apply gravity
			arrowMotionY -= gravity * 0.1;
			
			// check for collision
			RaycastContext context = new RaycastContext(eyesPos, arrowPos,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, MC.player);
			if(MC.world.raycast(context).getType() != HitResult.Type.MISS)
				break;
		}
		
		return path;
	}
	
	private double getProjectileGravity(Item item)
	{
		if(item instanceof BowItem || item instanceof CrossbowItem)
			return 0.05;
		
		if(item instanceof PotionItem)
			return 0.4;
		
		if(item instanceof FishingRodItem)
			return 0.15;
		
		if(item instanceof TridentItem)
			return 0.015;
		
		return 0.03;
	}
	
	private boolean isThrowable(Item item)
	{
		return item instanceof BowItem || item instanceof CrossbowItem
			|| item instanceof SnowballItem || item instanceof EggItem
			|| item instanceof EnderPearlItem
			|| item instanceof SplashPotionItem
			|| item instanceof LingeringPotionItem
			|| item instanceof FishingRodItem || item instanceof TridentItem;
	}
}
