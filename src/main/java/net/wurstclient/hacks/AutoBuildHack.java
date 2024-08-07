/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.*;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.json.JsonException;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener, RenderListener
{
	private final FileSetting templateSetting = new FileSetting("Template",
		"Determines what to build.\n\n"
			+ "Templates are just JSON files. Feel free to add your own or to edit / delete the default templates.\n\n"
			+ "If you mess up, simply press the 'Reset to Defaults' button or delete the folder.",
		"autobuild", DefaultAutoBuildTemplates::createFiles);
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far to reach when placing blocks.\n" + "Recommended values:\n"
			+ "6.0 for vanilla\n" + "4.25 for NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Makes sure that you don't reach through walls when placing blocks. Can help with AntiCheat plugins but slows down building.",
		false);
	
	private final CheckboxSetting instaBuild = new CheckboxSetting("InstaBuild",
		"Builds small templates (<= 64 blocks) instantly.\n"
			+ "For best results, stand close to the block you're placing.",
		true);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("Always FastPlace",
			"Builds as if FastPlace was enabled, even if it's not.", true);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashSet<BlockPos> remainingBlocks = new LinkedHashSet<>();
	
	public AutoBuildHack()
	{
		super("AutoBuild");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(checkLOS);
		addSetting(instaBuild);
		addSetting(fastPlace);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		switch(status)
		{
			case NO_TEMPLATE:
			break;
			
			case LOADING:
			name += " [Loading...]";
			break;
			
			case IDLE:
			name += " [" + template.getName() + "]";
			break;
			
			case BUILDING:
			double total = template.size();
			double placed = total - remainingBlocks.size();
			double progress = Math.round(placed / total * 1e4) / 1e2;
			name += " [" + template.getName() + "] " + progress + "%";
			break;
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().templateToolHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		remainingBlocks.clear();
		
		if(template == null)
			status = Status.NO_TEMPLATE;
		else
			status = Status.IDLE;
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(status != Status.IDLE)
			return;
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos = hitResultPos.offset(blockHitResult.getSide());
		Direction direction = MC.player.getHorizontalFacing();
		remainingBlocks = template.getPositions(startPos, direction);
		
		if(instaBuild.isChecked() && template.size() <= 64)
			buildInstantly();
		else
			status = Status.BUILDING;
	}
	
	@Override
	public void onUpdate()
	{
		switch(status)
		{
			case NO_TEMPLATE:
			loadSelectedTemplate();
			break;
			
			case LOADING:
			break;
			
			case IDLE:
			if(!template.isSelected(templateSetting))
				loadSelectedTemplate();
			break;
			
			case BUILDING:
			buildNormally();
			break;
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		matrixStack.push();
		
		RegionPos region = RenderUtils.getCameraRegion();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		RenderSystem.setShaderColor(0, 0, 0, 0.5F);
		RenderSystem.setShader(GameRenderer::getPositionProgram);
		
		double boxStart = 1 / 16.0;
		double boxEnd = 15 / 16.0;
		Box box = new Box(boxStart, boxStart, boxStart, boxEnd, boxEnd, boxEnd)
			.offset(region.negate().toBlockPos());
		
		ArrayList<BlockPos> blocksToDraw = remainingBlocks.stream()
			.filter(pos -> BlockUtils.getState(pos).isReplaceable()).limit(1024)
			.collect(Collectors.toCollection(ArrayList::new));
		
		GL11.glDepthMask(false);
		RenderSystem.setShaderColor(0, 1, 0, 0.15F);
		
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = range.getValueSq();
		blocksToDraw.stream()
			.filter(pos -> pos.getSquaredDistance(eyesPos) <= rangeSq)
			.map(pos -> box.offset(pos)).forEach(
				offsetBox -> RenderUtils.drawSolidBox(offsetBox, matrixStack));
		
		GL11.glDepthMask(true);
		RenderSystem.setShaderColor(0, 0, 0, 0.5F);
		
		blocksToDraw.stream().map(pos -> box.offset(pos)).forEach(
			offsetBox -> RenderUtils.drawOutlinedBox(offsetBox, matrixStack));
		
		matrixStack.pop();
		
		// GL resets
		GL11.glDisable(GL11.GL_BLEND);
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}
	
	private void buildNormally()
	{
		remainingBlocks
			.removeIf(pos -> !BlockUtils.getState(pos).isReplaceable());
		
		if(remainingBlocks.isEmpty())
		{
			status = Status.IDLE;
			return;
		}
		
		if(!fastPlace.isChecked() && MC.itemUseCooldown > 0)
			return;
		
		double rangeSq = range.getValueSq();
		for(BlockPos pos : remainingBlocks)
		{
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq)
				continue;
			if(checkLOS.isChecked() && !params.lineOfSight())
				continue;
			
			MC.itemUseCooldown = 4;
			RotationUtils.getNeededRotations(params.hitVec())
				.sendPlayerLookPacket();
			InteractionSimulator.rightClickBlock(params.toHitResult());
			break;
		}
	}
	
	private void buildInstantly()
	{
		double rangeSq = range.getValueSq();
		
		for(BlockPos pos : remainingBlocks)
		{
			if(!BlockUtils.getState(pos).isReplaceable())
				continue;
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq)
				continue;
			
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				SwingHand.OFF);
		}
		
		remainingBlocks.clear();
	}
	
	private void loadSelectedTemplate()
	{
		status = Status.LOADING;
		Path path = templateSetting.getSelectedFile();
		
		try
		{
			template = AutoBuildTemplate.load(path);
			status = Status.IDLE;
			
		}catch(IOException | JsonException e)
		{
			Path fileName = path.getFileName();
			ChatUtils.error("Couldn't load template '" + fileName + "'.");
			
			String simpleClassName = e.getClass().getSimpleName();
			String message = e.getMessage();
			ChatUtils.message(simpleClassName + ": " + message);
			
			e.printStackTrace();
			setEnabled(false);
		}
	}
	
	public Path getFolder()
	{
		return templateSetting.getFolder();
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE,
		BUILDING;
	}
}
