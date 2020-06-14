package net.wurstclient.hacks;
/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;


public class TrailHack extends Hack implements UpdateListener, RenderListener{
	
	private List<List<Vec3d>> trails = new ArrayList<>();
	
	private final SliderSetting size = new SliderSetting("Trail size",
			"How long the trail should be\n"
				+ "0 = infinite",
			0, 0, 1000, 1, ValueDisplay.INTEGER);
	
	public TrailHack() {
		super("Trail", "Shows a trail where you go");
		setCategory(Category.RENDER);
		addSetting(size);
	}


	@Override
	public void onEnable() {
		trails.clear();
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}

	@Override
	public void onUpdate() {
		if (trails.isEmpty()) trails.add(Arrays.asList(MC.player.getPos().add(0, 0.1, 0), MC.player.getPos()));
		else if (MC.player.getPos().add(0, 0.1, 0).distanceTo(Iterables.getLast(trails).get(0)) > 0.05) {
			trails.add(Arrays.asList(Iterables.getLast(trails).get(1), MC.player.getPos().add(0, 0.1, 0)));
			int last = -1;
			if (trails.size() > size.getValueI()) {
				if(size.getValueI() == 0)
					return;
				last++;
				while(trails.size() > size.getValueI())
					trails.remove(last);
			}
			else last = -1;
		}
	}

	
	@Override
	public void onRender(float partialTicks) {
		int count = 250;
		Color clr = new Color(0,0,0);
		boolean rev = false;
		for (List<Vec3d> e: trails) {
			clr = new Color(0, 255 - count, count);
			drawLine(e.get(0).x, e.get(0).y, e.get(0).z, e.get(1).x, e.get(1).y, e.get(1).z,
					clr.getRed()/255f, clr.getGreen()/255f, clr.getBlue()/255f,
					(float) 4);
			if (count < 5 || count > 250) rev = !rev;
			count += rev ? 3 : -3;
		}
	}
	
	private static void drawLine(double x1,double y1,double z1,double x2,double y2,double z2, float r, float g, float b, float t) {
		gl11Setup();
		GL11.glLineWidth(t);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(3, VertexFormats.POSITION_COLOR);
        buffer.vertex(x1, y1, z1).color(r, g, b, 0.0F).next();
        buffer.vertex(x1, y1, z1).color(r, g, b, 1.0F).next();
        buffer.vertex(x2, y2, z2).color(r, g, b, 1.0F).next();
        tessellator.draw();
        
		gl11Cleanup();
	}
	
	private static void gl11Setup() {
		GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glLineWidth(2.5F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glMatrixMode(5889);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glPushMatrix();
		offsetRender();
	}
	
	private static void gl11Cleanup() {
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glPopMatrix();
		GL11.glMatrixMode(5888);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}
	
	private static void offsetRender() {
		Camera camera = BlockEntityRenderDispatcher.INSTANCE.camera;
		Vec3d camPos = camera.getPos();
		GL11.glRotated(MathHelper.wrapDegrees(camera.getPitch()), 1, 0, 0);
		GL11.glRotated(MathHelper.wrapDegrees(camera.getYaw() + 180.0), 0, 1, 0);
		GL11.glTranslated(-camPos.x, -camPos.y, -camPos.z);
	}
}
