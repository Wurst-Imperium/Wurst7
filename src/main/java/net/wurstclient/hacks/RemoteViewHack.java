/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

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
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.FakePlayerEntity;

@SearchTags({"remote view"})
@DontSaveState
public final class RemoteViewHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final CheckboxSetting filterPlayers = new CheckboxSetting(
		"Filter players", "Won't view other players.", false);
	
	private final CheckboxSetting filterSleeping = new CheckboxSetting(
		"Filter sleeping", "Won't view sleeping players.", false);
	
	private final SliderSetting filterFlying =
		new SliderSetting("Filter flying",
			"Won't view players that\n" + "are at least the given\n"
				+ "distance above ground.",
			0, 0, 2, 0.05,
			v -> v == 0 ? "off" : ValueDisplay.DECIMAL.getValueString(v));
	
	private final CheckboxSetting filterMonsters = new CheckboxSetting(
		"Filter monsters", "Won't view zombies, creepers, etc.", true);
	
	private final CheckboxSetting filterPigmen =
		new CheckboxSetting("Filter pigmen", "Won't view zombie pigmen.", true);
	
	private final CheckboxSetting filterEndermen =
		new CheckboxSetting("Filter endermen", "Won't view endermen.", true);
	
	private final CheckboxSetting filterAnimals = new CheckboxSetting(
		"Filter animals", "Won't view pigs, cows, etc.", true);
	
	private final CheckboxSetting filterBabies =
		new CheckboxSetting("Filter babies",
			"Won't view baby pigs,\n" + "baby villagers, etc.", true);
	
	private final CheckboxSetting filterPets =
		new CheckboxSetting("Filter pets",
			"Won't view tamed wolves,\n" + "tamed horses, etc.", true);
	
	private final CheckboxSetting filterTraders =
		new CheckboxSetting("Filter traders",
			"Won't view villagers, wandering traders, etc.", true);
	
	private final CheckboxSetting filterGolems =
		new CheckboxSetting("Filter golems",
			"Won't view iron golems,\n" + "snow golems and shulkers.", true);
	
	private final CheckboxSetting filterInvisible = new CheckboxSetting(
		"Filter invisible", "Won't view invisible entities.", false);
	private final CheckboxSetting filterStands = new CheckboxSetting(
		"Filter armor stands", "Won't view armor stands.", true);
	
	private Entity entity = null;
	private boolean wasInvisible;
	
	private FakePlayerEntity fakePlayer;
	
	public RemoteViewHack()
	{
		super("RemoteView", "Allows you to see the world as someone else.\n"
			+ "Use the .rv command to make it target a specific entity.");
		setCategory(Category.RENDER);
		
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
	}
	
	@Override
	public void onEnable()
	{
		// find entity if not already set
		if(entity == null)
		{
			Stream<Entity> stream = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> !e.removed && ((LivingEntity)e).getHealth() > 0)
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
			
			entity = stream
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			// check if entity was found
			if(entity == null)
			{
				ChatUtils.error("Could not find a valid entity.");
				setEnabled(false);
				return;
			}
		}
		
		// save old data
		wasInvisible = entity.isInvisibleTo(MC.player);
		
		// enable NoClip
		MC.player.noClip = true;
		
		// spawn fake player
		fakePlayer = new FakePlayerEntity();
		
		// success message
		ChatUtils.message("Now viewing " + entity.getName().getString() + ".");
		
		// add listener
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		// remove listener
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		
		// reset entity
		if(entity != null)
		{
			ChatUtils.message(
				"No longer viewing " + entity.getName().getString() + ".");
			entity.setInvisible(wasInvisible);
			entity = null;
		}
		
		// disable NoClip
		MC.player.noClip = false;
		
		// remove fake player
		if(fakePlayer != null)
		{
			fakePlayer.resetPlayerPosition();
			fakePlayer.despawn();
		}
	}
	
	public void onToggledByCommand(String viewName)
	{
		// set entity
		if(!isEnabled() && viewName != null && !viewName.isEmpty())
		{
			entity = StreamSupport
				.stream(MC.world.getEntities().spliterator(), true)
				.filter(e -> e instanceof LivingEntity)
				.filter(e -> !e.removed && ((LivingEntity)e).getHealth() > 0)
				.filter(e -> e != MC.player)
				.filter(e -> !(e instanceof FakePlayerEntity))
				.filter(e -> viewName.equalsIgnoreCase(e.getName().getString()))
				.min(Comparator
					.comparingDouble(e -> MC.player.squaredDistanceTo(e)))
				.orElse(null);
			
			if(entity == null)
			{
				ChatUtils
					.error("Entity \"" + viewName + "\" could not be found.");
				return;
			}
		}
		
		// toggle RemoteView
		setEnabled(!isEnabled());
	}
	
	@Override
	public void onUpdate()
	{
		// validate entity
		if(entity.removed || ((LivingEntity)entity).getHealth() <= 0)
		{
			setEnabled(false);
			return;
		}
		
		// update position, rotation, etc.
		MC.player.copyPositionAndRotation(entity);
		MC.player.resetPosition(entity.getX(),
			entity.getY() - MC.player.getEyeHeight(MC.player.getPose())
				+ entity.getEyeHeight(entity.getPose()),
			entity.getZ());
		MC.player.setVelocity(Vec3d.ZERO);
		
		// set entity invisible
		entity.setInvisible(true);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
}
