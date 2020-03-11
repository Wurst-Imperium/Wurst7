/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import org.lwjgl.opengl.GL11;

import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnGlobalS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
		super("PlayerFinder", "Finds far away players during thunderstorms.");
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
	public void onRender(float partialTicks)
	{
		if(pos == null)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// generate rainbow color
		float x = System.currentTimeMillis() % 2000 / 1000F;
		float red = 0.5F + 0.5F * MathHelper.sin(x * (float)Math.PI);
		float green =
			0.5F + 0.5F * MathHelper.sin((x + 4F / 3F) * (float)Math.PI);
		float blue =
			0.5F + 0.5F * MathHelper.sin((x + 8F / 3F) * (float)Math.PI);
		
		GL11.glColor4f(red, green, blue, 0.5F);
		
		// tracer line
		GL11.glBegin(GL11.GL_LINES);
		{
			// set start position
			Vec3d start = RotationUtils.getClientLookVec()
				.add(RenderUtils.getCameraPos());
			
			// set end position
			Vec3d end = new Vec3d(pos).add(0.5, 0.5, 0.5);
			
			// draw line
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
		GL11.glEnd();
		
		// block box
		{
			GL11.glPushMatrix();
			GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
			
			RenderUtils.drawOutlinedBox();
			
			GL11.glColor4f(red, green, blue, 0.25F);
			RenderUtils.drawSolidBox();
			
			GL11.glPopMatrix();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
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
		if(packet instanceof PlaySoundS2CPacket)
		{
			PlaySoundS2CPacket sound = (PlaySoundS2CPacket)packet;
			newPos = new BlockPos(sound.getX(), sound.getY(), sound.getZ());
			
		}else if(packet instanceof EntitySpawnGlobalS2CPacket)
		{
			EntitySpawnGlobalS2CPacket lightning =
				(EntitySpawnGlobalS2CPacket)packet;
			newPos = new BlockPos(lightning.getX() / 32D,
				lightning.getY() / 32D, lightning.getZ() / 32D);
		}
		
		if(newPos == null)
			return;
		
		// check distance to player
		BlockPos playerPos = new BlockPos(MC.player);
		if(Math.abs(playerPos.getX() - newPos.getX()) > 256
			|| Math.abs(playerPos.getZ() - newPos.getZ()) > 256)
			pos = newPos;
	}
}
