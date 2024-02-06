/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.ai.PathFinder;
import net.wurstclient.ai.PathPos;
import net.wurstclient.ai.PathProcessor;
import net.wurstclient.commands.PathCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IKeyBinding;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"anti afk", "AFKBot", "afk bot"})
@DontSaveState
public final class AntiAfkHack extends Hack
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting useAi = new CheckboxSetting("Use AI", true);
	
	private int timer;
	private Random random = new Random();
	private BlockPos start;
	private BlockPos nextBlock;
	
	private RandomPathFinder pathFinder;
	private PathProcessor processor;
	private boolean creativeFlying;
	
	public AntiAfkHack()
	{
		super("AntiAFK");
		
		setCategory(Category.OTHER);
		addSetting(useAi);
	}
	
	@Override
	public void onEnable()
	{
		start = BlockPos.ofFloored(MC.player.getPos());
		nextBlock = null;
		pathFinder = new RandomPathFinder(start);
		creativeFlying = MC.player.getAbilities().flying;
		
		WURST.getHax().autoFishHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		((IKeyBinding)MC.options.forwardKey).resetPressedState();
		((IKeyBinding)MC.options.jumpKey).resetPressedState();
		
		pathFinder = null;
		processor = null;
		PathProcessor.releaseControls();
	}
	
	@Override
	public void onUpdate()
	{
		// check if player died
		if(MC.player.getHealth() <= 0)
		{
			setEnabled(false);
			return;
		}
		
		MC.player.getAbilities().flying = creativeFlying;
		
		if(useAi.isChecked())
		{
			// update timer
			if(timer > 0)
			{
				timer--;
				if(!WURST.getHax().jesusHack.isEnabled())
					MC.options.jumpKey.setPressed(MC.player.isTouchingWater());
				return;
			}
			
			// find path
			if(!pathFinder.isDone() && !pathFinder.isFailed())
			{
				PathProcessor.lockControls();
				
				pathFinder.think();
				
				if(!pathFinder.isDone() && !pathFinder.isFailed())
					return;
				
				pathFinder.formatPath();
				
				// set processor
				processor = pathFinder.getProcessor();
			}
			
			// check path
			if(processor != null
				&& !pathFinder.isPathStillValid(processor.getIndex()))
			{
				pathFinder = new RandomPathFinder(pathFinder);
				return;
			}
			
			// process path
			if(!processor.isDone())
				processor.process();
			else
				pathFinder = new RandomPathFinder(start);
			
			// wait 2 - 3 seconds (40 - 60 ticks)
			if(processor.isDone())
			{
				PathProcessor.releaseControls();
				timer = 40 + random.nextInt(21);
			}
		}else
		{
			// set next block
			if(timer <= 0 || nextBlock == null)
			{
				nextBlock =
					start.add(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
				timer = 40 + random.nextInt(21);
			}
			
			// face block
			WURST.getRotationFaker()
				.faceVectorClientIgnorePitch(Vec3d.ofCenter(nextBlock));
			
			// walk
			if(MC.player.squaredDistanceTo(Vec3d.ofCenter(nextBlock)) > 0.5)
				MC.options.forwardKey.setPressed(true);
			else
				MC.options.forwardKey.setPressed(false);
			
			// swim up
			MC.options.jumpKey.setPressed(MC.player.isTouchingWater());
			
			// update timer
			if(timer > 0)
				timer--;
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(!useAi.isChecked())
			return;
		
		PathCmd pathCmd = WURST.getCmds().pathCmd;
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		pathFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
			pathCmd.isDepthTest());
	}
	
	private class RandomPathFinder extends PathFinder
	{
		public RandomPathFinder(BlockPos goal)
		{
			super(goal.add(random.nextInt(33) - 16, random.nextInt(33) - 16,
				random.nextInt(33) - 16));
			setThinkTime(10);
			setFallingAllowed(false);
			setDivingAllowed(false);
		}
		
		public RandomPathFinder(PathFinder pathFinder)
		{
			super(pathFinder);
			setFallingAllowed(false);
			setDivingAllowed(false);
		}
		
		@Override
		public ArrayList<PathPos> formatPath()
		{
			failed = true;
			return super.formatPath();
		}
	}
}
