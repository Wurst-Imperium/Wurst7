/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
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
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"anti afk", "AFKBot", "afk bot"})
@DontSaveState
public final class AntiAfkHack extends Hack
	implements UpdateListener, RenderListener
{
	private final CheckboxSetting useAi = new CheckboxSetting("Use AI",
		"description.wurst.setting.antiafk.use_ai", true);
	
	private final SliderSetting aiRange = new SliderSetting("AI range",
		"description.wurst.setting.antiafk.ai_range", 16, 1, 64, 1,
		ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting nonAiRange = new SliderSetting("Non-AI range",
		"description.wurst.setting.antiafk.non-ai_range", 1, 1, 64, 1,
		ValueDisplay.AREA_FROM_RADIUS);
	
	private final SliderSetting waitTime = new SliderSetting("Wait time",
		"description.wurst.setting.antiafk.wait_time", 2.5, 0, 60, 0.05,
		ValueDisplay.DECIMAL.withSuffix("s"));
	
	private final SliderSetting waitTimeRand = new SliderSetting(
		"Wait time randomization",
		"description.wurst.setting.antiafk.wait_time_randomization", 0.5, 0, 60,
		0.05, ValueDisplay.DECIMAL.withPrefix("\u00b1").withSuffix("s"));
	
	private final CheckboxSetting showWaitTime =
		new CheckboxSetting("Show wait time",
			"description.wurst.setting.antiafk.show_wait_time", true);
	
	private int timer;
	private Random random = Random.createLocal();
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
		addSetting(showWaitTime);
	}
	
	@Override
	public String getRenderName()
	{
		if(showWaitTime.isChecked() && timer > 0)
			return getName() + " [" + timer * 50 + "ms]";
		
		return getName();
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
		PathProcessor.releaseControls();
		pathFinder = null;
		processor = null;
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
			// prevent drowning
			if(MC.player.isSubmergedInWater()
				&& !WURST.getHax().jesusHack.isEnabled())
			{
				MC.options.jumpKey.setPressed(true);
				return;
			}
			
			// update timer
			if(timer > 0)
			{
				timer--;
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
				&& !pathFinder.isPathStillValid(processor.getIndex())
				|| processor.getTicksOffPath() > 20)
			{
				pathFinder = new RandomPathFinder(pathFinder);
				return;
			}
			
			// process path
			if(!processor.isDone())
				processor.process();
			else
			{
				// reset and wait for timer
				PathProcessor.releaseControls();
				pathFinder = new RandomPathFinder(
					randomize(start, aiRange.getValueI(), true));
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
		pathFinder.renderPath(matrixStack, pathCmd.isDebugMode(),
			pathCmd.isDepthTest());
	}
	
	private void setTimer()
	{
		int baseTime = (int)(waitTime.getValue() * 20);
		double randTime = waitTimeRand.getValue() * 20;
		int randOffset = (int)(random.nextGaussian() * randTime);
		randOffset = Math.max(randOffset, -baseTime);
		timer = baseTime + randOffset;
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
