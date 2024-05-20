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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"anti afk", "AFKBot", "afk bot"})
@DontSaveState
public final class AntiAfkHack extends Hack
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting useAi = new CheckboxSetting("Use AI",
		"Uses a pathfinding AI to move around naturally and avoid hazards.\n"
			+ "Can sometimes get stuck.",
		true);
	
	private final SliderSetting aiRange = new SliderSetting("AI range",
		"The area in which AntiAFK can move when Use AI is turned on.", 16, 1,
		64, 1, ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting nonAiRange = new SliderSetting("Non-AI range",
		"The area in which AntiAFK can move when Use AI is turned off.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r This area must be completely"
			+ " unobstructed and free of hazards.",
		1, 1, 64, 1, ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting waitTime =
		new SliderSetting("Wait time", "Time between movements in seconds.",
			2.5, 0, 60, 0.05, ValueDisplay.DECIMAL.withSuffix("s"));
	
	private final SliderSetting waitTimeRand =
		new SliderSetting("Wait time randomization",
			"How much time can be randomly added or subtracted from the wait"
				+ " time, in seconds.",
			0.5, 0, 60, 0.05,
			ValueDisplay.DECIMAL.withPrefix("\u00b1").withSuffix("s"));
	
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
		addSetting(aiRange);
		addSetting(nonAiRange);
		addSetting(waitTime);
		addSetting(waitTimeRand);
	}
	
	@Override
	protected void onEnable()
	{
		start = BlockPos.ofFloored(MC.player.getPos());
		nextBlock = null;
		pathFinder =
			new RandomPathFinder(randomize(start, aiRange.getValueI(), true));
		creativeFlying = MC.player.getAbilities().flying;
		
		WURST.getHax().autoFishHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		IKeyBinding.get(MC.options.forwardKey).resetPressedState();
		IKeyBinding.get(MC.options.jumpKey).resetPressedState();
		
		pathFinder = null;
		processor = null;
		PathProcessor.releaseControls();
	}
	
	private void setTimer()
	{
		int baseTime = (int)(waitTime.getValue() * 20);
		int randTime = (int)(waitTimeRand.getValue() * 20);
		int randOffset = random.nextInt(randTime * 2 + 1) - randTime;
		randOffset = Math.max(randOffset, -baseTime);
		timer = baseTime + randOffset;
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
				pathFinder = new RandomPathFinder(
					randomize(start, aiRange.getValueI(), true));
			
			// wait 2 - 3 seconds (40 - 60 ticks)
			if(processor.isDone())
			{
				PathProcessor.releaseControls();
				setTimer();
			}
		}else
		{
			// set next block
			if(timer <= 0 || nextBlock == null)
			{
				nextBlock = randomize(start, nonAiRange.getValueI(), false);
				setTimer();
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
	
	private BlockPos randomize(BlockPos pos, int range, boolean includeY)
	{
		int x = random.nextInt(2 * range + 1) - range;
		int y = includeY ? random.nextInt(2 * range + 1) - range : 0;
		int z = random.nextInt(2 * range + 1) - range;
		return pos.add(x, y, z);
	}
	
	private class RandomPathFinder extends PathFinder
	{
		public RandomPathFinder(BlockPos goal)
		{
			super(goal);
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
