/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.FakePlayerEntity;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"player esp", "PlayerTracers", "player tracers"})
public final class PlayerEspHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.LINES_AND_BOXES);
	
	private final EnumSetting<BoxSize> boxSize = new EnumSetting<>("Box size",
		"\u00a7lAccurate\u00a7r mode shows the exact\n"
			+ "hitbox of each player.\n"
			+ "\u00a7lFancy\u00a7r mode shows slightly larger\n"
			+ "boxes that look better.",
		BoxSize.values(), BoxSize.FANCY);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't show sleeping players.", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't show invisible players.", false);

	private final CheckboxSetting filterNonFriends = new CheckboxSetting(
			"Filter non-friends", "Won't show players that are not in your friends list", false);
	
	private int playerBox;
	private final ArrayList<PlayerEntity> players = new ArrayList<>();
	
	public PlayerEspHack()
	{
		super("PlayerESP", "Highlights nearby players.\n"
			+ "ESP boxes of friends will appear in blue.");
		setCategory(Category.RENDER);
		
		addSetting(style);
		addSetting(boxSize);
		addSetting(filterSleeping);
		addSetting(filterInvisible);
		addSetting(filterNonFriends);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		playerBox = GL11.glGenLists(1);
		GL11.glNewList(playerBox, GL11.GL_COMPILE);
		Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		GL11.glDeleteLists(playerBox, 1);
		playerBox = 0;
	}
	
	@Override
	public void onUpdate()
	{
		PlayerEntity player = MC.player;
		ClientWorld world = MC.world;
		
		players.clear();
		Stream<AbstractClientPlayerEntity> stream = world.getPlayers()
			.parallelStream().filter(e -> !e.removed && e.getHealth() > 0)
			.filter(e -> e != player)
			.filter(e -> !(e instanceof FakePlayerEntity))
			.filter(e -> Math.abs(e.getY() - MC.player.getY()) <= 1e6);
		
		if(filterSleeping.isChecked())
			stream = stream.filter(e -> !e.isSleeping());
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());

		if (filterNonFriends.isChecked())
			stream = stream.filter(e -> WURST.getFriends().contains(e.getName().getString()));
		
		players.addAll(stream.collect(Collectors.toList()));
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// draw boxes
		if(style.getSelected().boxes)
			renderBoxes(partialTicks);
		
		if(style.getSelected().lines)
			renderTracers(partialTicks);
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void renderBoxes(double partialTicks)
	{
		double extraSize = boxSize.getSelected().extraSize;
		
		for(PlayerEntity e : players)
		{
			GL11.glPushMatrix();
			
			GL11.glTranslated(e.prevX + (e.getX() - e.prevX) * partialTicks,
				e.prevY + (e.getY() - e.prevY) * partialTicks,
				e.prevZ + (e.getZ() - e.prevZ) * partialTicks);
			
			GL11.glScaled(e.getWidth() + extraSize, e.getHeight() + extraSize,
				e.getWidth() + extraSize);
			
			// set color
			if(WURST.getFriends().contains(e.getEntityName()))
				GL11.glColor4f(0, 0, 1, 0.5F);
			else
			{
				float f = MC.player.distanceTo(e) / 20F;
				GL11.glColor4f(2 - f, f, 0, 0.5F);
			}
			
			GL11.glCallList(playerBox);
			
			GL11.glPopMatrix();
		}
	}
	
	private void renderTracers(double partialTicks)
	{
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos());
		
		GL11.glBegin(GL11.GL_LINES);
		for(PlayerEntity e : players)
		{
			Vec3d end = e.getBoundingBox().getCenter()
				.subtract(new Vec3d(e.getX(), e.getY(), e.getZ())
					.subtract(e.prevX, e.prevY, e.prevZ)
					.multiply(1 - partialTicks));
			
			if(WURST.getFriends().contains(e.getEntityName()))
				GL11.glColor4f(0, 0, 1, 0.5F);
			else
			{
				float f = MC.player.distanceTo(e) / 20F;
				GL11.glColor4f(2 - f, f, 0, 0.5F);
			}
			
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
		GL11.glEnd();
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private enum BoxSize
	{
		ACCURATE("Accurate", 0),
		FANCY("Fancy", 0.1);
		
		private final String name;
		private final double extraSize;
		
		private BoxSize(String name, double extraSize)
		{
			this.name = name;
			this.extraSize = extraSize;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
