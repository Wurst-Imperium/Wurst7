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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.HorseBaseEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
	
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"Filter players", "Won't follow other players.", false);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't follow sleeping players.", false);
	
	private final SliderSetting filterFlying =
		new SliderSetting("Filter flying",
			"Won't follow players that\n" + "are at least the given\n"
				+ "distance above ground.",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));
	
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"Filter monsters", "Won't follow zombies, creepers, etc.", true);
	
	private final CheckboxSetting filterPigmen = new CheckboxSetting(
		"Filter pigmen", "Won't follow zombie pigmen.", true);
	
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("Filter endermen", "Won't follow endermen.", true);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"Filter animals", "Won't follow pigs, cows, etc.", true);
	
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("Filter babies",
			"Won't follow baby pigs,\n" + "baby villagers, etc.", true);
	
	private final CheckboxSetting filterPets =
		new CheckboxSetting("Filter pets",
			"Won't follow tamed wolves,\n" + "tamed horses, etc.", true);
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("Filter traders",
			"Won't follow villagers, wandering traders, etc.", true);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't follow iron golems,\n" + "snow golems and shulkers.", true);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't follow invisible entities.", false);
	private final CheckboxSetting filterStands = new CheckboxSetting(
		"Filter armor stands", "Won't follow armor stands.", true);
	
	private final CheckboxSetting filterCarts = new CheckboxSetting(
		"Filter minecarts", "Won't follow minecarts.", true);
	
	public FollowHack()
	{
		super("Follow",
			"A bot that follows the closest entity.\n" + "Very annoying.\n\n"
				+ "Use .follow to follow a specific entity.");
		
		setCategory(Category.MOVEMENT);
		addSetting(distance);
		addSetting(useAi);
		
		addSetting(filterPlayers);
		addSetting(filterSleeping);
		addSetting(filterFlying);
		addSetting(filterMonsters);
		addSetting(filterPigmen);
		addSetting(filterEndermen);
		addSetting(filterAnimals);
		addSetting(filterBabies);
		addSetting(filterPets);
		addSetting(filterTraders);
		addSetting(filterGolems);
		addSetting(filterInvisible);
		addSetting(filterStands);
		addSetting(filterCarts);
	}
	
	@Override
	public String getRenderName()
	{
		if(entity != null)
			return "Following " + entity.getName().getString();
		else
			return "Follow";
	}
	
	@Override
	public void onEnable()
	{
		if(entity == null)
		{
			Stream<Entity> stream =
				StreamSupport.stream(MC.world.getEntities().spliterator(), true)
					.filter(e -> !e.removed)
					.filter(e -> e instanceof LivingEntity
						&& ((LivingEntity)e).getHealth() > 0
						|| e instanceof AbstractMinecartEntity)
					.filter(e -> e != MC.player)
					.filter(e -> !(e instanceof FakePlayerEntity));
			
			if(filterPlayers.isChecked())
				stream = stream.filter(e -> !(e instanceof PlayerEntity));
			
			if(filterSleeping.isChecked())
				stream = stream.filter(e -> !(e instanceof PlayerEntity
					&& ((PlayerEntity)e).isSleeping()));
			
			if(filterFlying.getValue() > 0)
				stream = stream.filter(e -> {
					
					if(!(e instanceof PlayerEntity))
						return true;
					
					Box box = e.getBoundingBox();
					box = box.union(box.offset(0, -filterFlying.getValue(), 0));
					return MC.world.isSpaceEmpty(box);
				});
			
			if(filterMonsters.isChecked())
				stream = stream.filter(e -> !(e instanceof Monster));
			
			if(filterPigmen.isChecked())
				stream =
					stream.filter(e -> !(e instanceof ZombifiedPiglinEntity));
			
			if(filterEndermen.isChecked())
				stream = stream.filter(e -> !(e instanceof EndermanEntity));
			
			if(filterAnimals.isChecked())
				stream = stream.filter(e -> !(e instanceof AnimalEntity
					|| e instanceof AmbientEntity
					|| e instanceof WaterCreatureEntity));
			
			if(filterBabies.isChecked())
				stream = stream.filter(e -> !(e instanceof PassiveEntity
					&& ((PassiveEntity)e).isBaby()));
			
			if(filterPets.isChecked())
				stream = stream
					.filter(e -> !(e instanceof TameableEntity
						&& ((TameableEntity)e).isTamed()))
					.filter(e -> !(e instanceof HorseBaseEntity
						&& ((HorseBaseEntity)e).isTame()));
			
			if(filterTraders.isChecked())
				stream = stream.filter(e -> !(e instanceof MerchantEntity));
			
			if(filterGolems.isChecked())
				stream = stream.filter(e -> !(e instanceof GolemEntity));
			
			if(filterInvisible.isChecked())
				stream = stream.filter(e -> !e.isInvisible());
			
			if(filterStands.isChecked())
				stream = stream.filter(e -> !(e instanceof ArmorStandEntity));
			
			if(filterCarts.isChecked())
				stream =
					stream.filter(e -> !(e instanceof AbstractMinecartEntity));
			
			entity = stream
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
		ChatUtils.message("Now following " + entity.getName().getString());
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
				.message("No longer following " + entity.getName().getString());
		
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
		if(entity.removed || entity instanceof LivingEntity
			&& ((LivingEntity)entity).getHealth() <= 0)
		{
			entity = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> !e.removed && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> entity.getName().getString()
					.equalsIgnoreCase(e.getName().getString()))
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
			if(MC.player.horizontalCollision && MC.player.isOnGround())
				MC.player.jump();
			
			// swim up if necessary
			if(MC.player.isTouchingWater() && MC.player.getY() < entity.getY())
				MC.player.setVelocity(MC.player.getVelocity().add(0, 0.04, 0));
			
			// control height if flying
			if(!MC.player.isOnGround()
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
			super(new BlockPos(entity.getPos()));
			setThinkTime(1);
		}
		
		@Override
		protected boolean checkDone()
		{
			Vec3d center = Vec3d.ofCenter(current);
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
