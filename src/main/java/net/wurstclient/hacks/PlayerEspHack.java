/*
 * Copyright (C) 2014 - 2019 | WURST-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.wurstclient.WurstClient;
import net.wurstclient.util.ColorCode;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
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

	private final EnumSetting<Color> color = new EnumSetting<>("Color",
			"Range: The default style. Player colors are rendered according to their range.\n"
					+ "Fixed: All players are rendered in green. Friends are rendered in blue.\n"
					+ "Team: Players are rendered according to their team. Friends are rendered in rainbow.\n"
					+ "Team note: As many servers do not utilize the scoreboard team system, colors are guessed\n"
					+ "by the player's name tag color. This is not always accurate. Sorry!"
			, Color.values(), Color.RANGE);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't show sleeping players.", false);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't show invisible players.", false);
	
	private int playerBox;
	private final ArrayList<PlayerEntity> players = new ArrayList<>();
	
	public PlayerEspHack()
	{
		super("PlayerESP", "Highlights nearby players.\n"
			+ "ESP boxes of friends will appear in blue.");
		setCategory(Category.RENDER);
		
		addSetting(style);
		addSetting(boxSize);
		addSetting(color);
		addSetting(filterSleeping);
		addSetting(filterInvisible);
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
			.filter(e -> Math.abs(e.y - MC.player.y) <= 1e6);
		
		if(filterSleeping.isChecked())
			stream = stream.filter(e -> !e.isSleeping());
		
		if(filterInvisible.isChecked())
			stream = stream.filter(e -> !e.isInvisible());
		
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
			
			GL11.glTranslated(e.prevX + (e.x - e.prevX) * partialTicks,
				e.prevY + (e.y - e.prevY) * partialTicks,
				e.prevZ + (e.z - e.prevZ) * partialTicks);
			
			GL11.glScaled(e.getWidth() + extraSize, e.getHeight() + extraSize,
				e.getWidth() + extraSize);
			
			List<String> friends = WurstClient.INSTANCE.getFriendsList().getAllFriends();
			// Render color
			if (color.getSelected() == Color.RANGE)
			{
				float f = MC.player.distanceTo(e) / 20F;
				GL11.glColor4f(2 - f, f, 0, 0.5F);
			}
			else if (color.getSelected() == Color.FIXED)
			{
				if (friends.contains(e.getName().asString()))
				{
					GL11.glColor4f(0, 0.2f, 1, 0.5f);
				}
				else
				{
					GL11.glColor4f(0, 1, 0.2f, 0.5f);
				}
			}
			else if (color.getSelected() == Color.TEAM)
			{
				if (friends.contains(e.getName().asString()))
				{
					// Generates a rainbow color.
					int rgb = java.awt.Color.HSBtoRGB(
							(float) ((double)System.currentTimeMillis() / 1000.0d % 1), // Color iterates fully every millis / x milliseconds
							1, 1);
					java.awt.Color color = new java.awt.Color(rgb);
					ColorCode colorCode = new ColorCode(color.getRed(), color.getGreen(), color.getBlue()); // Can transform color from 0-255 to 0-1.

					GL11.glColor4f(colorCode.r, colorCode.g, colorCode.b, 0.5f);
				}
				else
				{
					ColorCode colorCode = GetBestColorCode(e.getDisplayName().asFormattedString());
					GL11.glColor4f(colorCode.r, colorCode.g, colorCode.b, 0.5f);
				}
			}
			
			GL11.glCallList(playerBox);
			
			GL11.glPopMatrix();
		}
	}
	
	private void renderTracers(double partialTicks)
	{
		Vec3d start = RotationUtils.getClientLookVec().add(
			BlockEntityRenderDispatcher.renderOffsetX,
			BlockEntityRenderDispatcher.renderOffsetY,
			BlockEntityRenderDispatcher.renderOffsetZ);
		
		GL11.glBegin(GL11.GL_LINES);
		for(PlayerEntity e : players)
		{
			Vec3d end = e.getBoundingBox().getCenter().subtract(
				new Vec3d(e.x, e.y, e.z).subtract(e.prevX, e.prevY, e.prevZ)
					.multiply(1 - partialTicks));

			List<String> friends = WurstClient.INSTANCE.getFriendsList().getAllFriends();
			// Render color
			if (color.getSelected() == Color.RANGE)
			{
				float f = MC.player.distanceTo(e) / 20F;
				GL11.glColor4f(2 - f, f, 0, 0.5F);
			}
			else if (color.getSelected() == Color.FIXED)
			{
				if (friends.contains(e.getName().asString()))
				{
					GL11.glColor4f(0, 0.2f, 1, 0.5f);
				}
				else
				{
					GL11.glColor4f(0, 1, 0.2f, 0.5f);
				}
			}
			else if (color.getSelected() == Color.TEAM)
			{
				if (friends.contains(e.getName().asString()))
				{
					// Generates a rainbow color.
					int rgb = java.awt.Color.HSBtoRGB(
							(float) ((double)System.currentTimeMillis() / 1000.0d % 1), // Color iterates fully every millis / x milliseconds
							1, 1);
					java.awt.Color color = new java.awt.Color(rgb);
					ColorCode colorCode = new ColorCode(color.getRed(), color.getGreen(), color.getBlue()); // Can transform color from 0-255 to 0-1.

					GL11.glColor4f(colorCode.r, colorCode.g, colorCode.b, 0.5f);
				}
				else
				{
					ColorCode colorCode = GetBestColorCode(e.getDisplayName().asFormattedString());
					GL11.glColor4f(colorCode.r, colorCode.g, colorCode.b, 0.5f);
				}
			}
			
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
		GL11.glEnd();
	}

	// The following methods are used to determine the player's team color without using the scoreboard.
	// As this code was written when I was a beginner, the quality is quite dubious.
	// Despite its unnecessary complexity, it works, and I've little desire to refactor it, so I've simply updated the comments.

	// OLD: Feed output of getPlayerName into this
	// NEW: Feed an entity's displayName into this.
	private static ColorCode GetBestColorCode(String text)
	{
		// https://stackoverflow.com/questions/6020384/create-array-of-regex-matches
		List<String> allMatches = new ArrayList<String>(); // Each item in list is one character.

		Matcher m = Pattern.compile("(?<=\\xa7).").matcher(text);

		while (m.find())
		{
			allMatches.add(m.group());
		}

		// Try to find applicable character to feed into ColorCode().
		// If color code is modifier, go to next. If none, or list was empty, this loop skips, and returns light purple by default.
		while (allMatches.size() > 0)
		{
			if (allMatches.get(allMatches.size() - 1).matches("[0-9a-f]")) // Character is 0-9 or a-f; that is, it is valid
			{
				return new ColorCode(allMatches.get(allMatches.size() - 1));
			}

			allMatches.remove(allMatches.size() - 1); // If did not match from back, remove element and try again.
		}
		// No matches left or none applicable. Return default.

		return new ColorCode("d");
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


	private enum Color
	{
		RANGE("Range", true, false, false),
		FIXED("Fixed", false, true, false),
		TEAM("Team", false, false, true);

		private final String name;
		private final boolean range;
		private final boolean fixed;
		private final boolean team;


		private Color(String name, boolean range, boolean fixed, boolean team)
		{
			this.name = name;
			this.range = range;
			this.fixed = fixed;
			this.team = team;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
