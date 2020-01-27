/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.StreamSupport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;

@DontSaveState
public final class FollowHack extends Hack
	implements UpdateListener, RenderListener
{
	private Entity entity;
	private EntityPathFinder pathFinder;
	private PathProcessor processor;
	private int ticksProcessing;
	
	private final SliderSetting distance =
		new SliderSetting("Distance", "How closely to follow the target.", 1, 1,
			12, 0.5, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting useAi =
		new CheckboxSetting("Use AI (experimental)", false);
	
	public FollowHack()
	{
		super("Follow",
			"A bot that follows the closest entity.\n" + "Very annoying.");
		
		setCategory(Category.MOVEMENT);
		addSetting(distance);
		addSetting(useAi);
	}
	
	@Override
	public String getRenderName()
	{
		if(entity != null)
			return "Following " + entity.getName().asString();
		else
			return "Follow";
	}
	
	@Override
	public void onEnable()
	{
		if(entity == null)
		{
			entity = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> !e.removed && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			if(entity == null)
			{
				ChatUtils.error("Could not find a valid entity.");
				setEnabled(false);
				return;
			}
		}
		
		pathFinder = new EntityPathFinder();
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
		ChatUtils.message("Now following " + entity.getName().asString());
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		pathFinder = null;
		processor = null;
		ticksProcessing = 0;
		PathProcessor.releaseControls();
		
		if(entity != null)
			ChatUtils
				.message("No longer following " + entity.getName().asString());
		
		entity = null;
	}
	
	@Override
	public void onUpdate()
	{
		// check if player died
		if(MC.player.getHealth() <= 0)
		{
			if(entity == null)
				ChatUtils.message("No longer following entity");
			setEnabled(false);
			return;
		}
		
		// check if entity died or disappeared
		if(entity.removed || ((LivingEntity)entity).getHealth() <= 0)
		{
			entity = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> !e.removed && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> entity.getName().asString()
					.equalsIgnoreCase(e.getName().asString()))
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			if(entity == null)
			{
				ChatUtils.message("No longer following entity");
				setEnabled(false);
				return;
			}
			
			pathFinder = new EntityPathFinder();
			processor = null;
			ticksProcessing = 0;
		}
		
		if(useAi.isChecked())
		{
			// reset pathfinder
			if((processor == null || processor.isDone() || ticksProcessing >= 10
				|| !pathFinder.isPathStillValid(processor.getIndex()))
				&& (pathFinder.isDone() || pathFinder.isFailed()))
			{
				pathFinder = new EntityPathFinder();
				processor = null;
				ticksProcessing = 0;
			}
			
			// find path
			if(!pathFinder.isDone() && !pathFinder.isFailed())
			{
				PathProcessor.lockControls();
				WURST.getRotationFaker()
					.faceVectorClient(entity.getBoundingBox().getCenter());
				pathFinder.think();
				pathFinder.formatPath();
				processor = pathFinder.getProcessor();
			}
			
			// process path
			if(!processor.isDone())
			{
				processor.process();
				ticksProcessing++;
			}
		}else
		{
			// jump if necessary
			if(MC.player.horizontalCollision && MC.player.onGround)
				MC.player.jump();
			
			// swim up if necessary
			if(MC.player.isTouchingWater() && MC.player.getY() < entity.getY())
				MC.player.setVelocity(MC.player.getVelocity().add(0, 0.04, 0));
			
			// control height if flying
			if(!MC.player.onGround
				&& (MC.player.abilities.flying
					|| WURST.getHax().flightHack.isEnabled())
				&& MC.player.squaredDistanceTo(entity.getX(), MC.player.getY(),
					entity.getZ()) <= MC.player.squaredDistanceTo(
						MC.player.getX(), entity.getY(), MC.player.getZ()))
			{
				if(MC.player.getY() > entity.getY() + 1D)
					MC.options.keySneak.setPressed(true);
				else if(MC.player.getY() < entity.getY() - 1D)
					MC.options.keyJump.setPressed(true);
			}else
			{
				MC.options.keySneak.setPressed(false);
				MC.options.keyJump.setPressed(false);
			}
			
			// follow entity
			WURST.getRotationFaker()
				.faceVectorClient(entity.getBoundingBox().getCenter());
			double distanceSq = Math.pow(distance.getValue(), 2);
			MC.options.keyForward
				.setPressed(MC.player.squaredDistanceTo(entity.getX(),
					MC.player.getY(), entity.getZ()) > distanceSq);
		}
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		pathFinder.renderPath(pathCmd.isDebugMode(), pathCmd.isDepthTest());
	}
	
	public void setEntity(Entity entity)
	{
		this.entity = entity;
	}
	
	private class EntityPathFinder extends PathFinder
	{
		public EntityPathFinder()
		{
			super(new BlockPos(entity));
			setThinkTime(1);
		}
		
		@Override
		protected boolean checkDone()
		{
			Vec3d center = new Vec3d(current).add(0.5, 0.5, 0.5);
			double distanceSq = Math.pow(distance.getValue(), 2);
			return done = entity.squaredDistanceTo(center) <= distanceSq;
		}
		
		@Override
		public ArrayList<PathPos> formatPath()
		{
			if(!done)
				failed = true;
			
			return super.formatPath();
		}
	}
}
