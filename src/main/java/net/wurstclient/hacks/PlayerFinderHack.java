/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketInputListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"player finder"})
public final class PlayerFinderHack extends Hack
	implements UpdateListener, PacketInputListener, RenderListener
{
	private BlockPos pos;
	private BlockPos lastPos;
	
	public PlayerFinderHack()
	{
		super("PlayerFinder");
		setCategory(Category.RENDER);
	}
	
	@Override
	public void onEnable()
	{
		pos = null;
		lastPos = null;
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketInputListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketInputListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(pos == null || pos.equals(lastPos))
			return;
		
		ChatUtils.message("PlayerFinder has detected a player near "
			+ pos.toShortString() + ".");
		lastPos = pos;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(pos == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack);
		
		BlockPos camPos = RenderUtils.getCameraBlockPos();
		int regionX = (camPos.getX() >> 9) * 512;
		int regionZ = (camPos.getZ() >> 9) * 512;
		
		float[] rainbow = RenderUtils.getRainbowColor();
		RenderSystem.setShaderColor(rainbow[0], rainbow[1], rainbow[2], 0.5F);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		// tracer line
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		
		// set start position
		Vec3d start = RotationUtils.getClientLookVec()
			.add(RenderUtils.getCameraPos()).subtract(regionX, 0, regionZ);
		
		// set end position
		Vec3d end = Vec3d.ofCenter(pos).subtract(regionX, 0, regionZ);
		
		// draw line
		bufferBuilder
			.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
			.next();
		bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
			.next();
		
		tessellator.draw();
		
		// block box
		{
			matrixStack.push();
			matrixStack.translate(pos.getX() - regionX, pos.getY(),
				pos.getZ() - regionZ);
			
			RenderUtils.drawOutlinedBox(matrixStack);
			
			RenderSystem.setShaderColor(rainbow[0], rainbow[1], rainbow[2],
				0.25F);
			RenderUtils.drawSolidBox(matrixStack);
			
			matrixStack.pop();
		}
		
		matrixStack.pop();
		
		// GL resets
		RenderSystem.setShaderColor(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	@Override
	public void onReceivedPacket(PacketInputEvent event)
	{
		if(MC.player == null)
			return;
		
		Packet<?> packet = event.getPacket();
		
		// get packet position
		BlockPos newPos = null;
		// if(packet instanceof SPacketEffect)
		// {
		// SPacketEffect effect = (SPacketEffect)packet;
		// newPos = effect.getSoundPos();
		//
		// }else
		if(packet instanceof PlaySoundS2CPacket sound)
			newPos =
				BlockPos.ofFloored(sound.getX(), sound.getY(), sound.getZ());
		
		if(newPos == null)
			return;
		
		// check distance to player
		BlockPos playerPos = BlockPos.ofFloored(MC.player.getPos());
		if(Math.abs(playerPos.getX() - newPos.getX()) > 256
			|| Math.abs(playerPos.getZ() - newPos.getZ()) > 256)
			pos = newPos;
	}
}
