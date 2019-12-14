/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashSet;

import org.lwjgl.opengl.GL11;

import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IClientPlayerInteractionManager;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.AutoBuildTemplate;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.DefaultAutoBuildTemplates;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RotationUtils.Rotation;
import net.wurstclient.util.json.JsonException;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener, RenderListener
{
	private final FileSetting templateSetting =
		new FileSetting("Template", "Determines what to build.", "autobuild",
			folder -> DefaultAutoBuildTemplates.createFiles(folder));
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far to reach when placing blocks.\n" + "Recommended values:\n"
			+ "6.0 for vanilla\n" + "4.25 for NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting instaBuild = new CheckboxSetting("InstaBuild",
		"Builds small templates (<= 64 blocks) instantly.\n"
			+ "Turn this off if your template is not\n"
			+ "being built correctly.",
		true);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
	
	public AutoBuildHack()
	{
		super("AutoBuild", "Builds things automatically.");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(instaBuild);
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
			name += " [" + template.getName() + "] " + positions.size();
			break;
		}
		
		return name;
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		positions.clear();
		
		if(template == null)
			status = Status.NO_TEMPLATE;
		else
			status = Status.IDLE;
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
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(status != Status.IDLE)
			return;
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getPos() == null
			|| hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult))
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)hitResult;
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos = hitResultPos.offset(blockHitResult.getSide());
		Direction direction = MC.player.getHorizontalFacing();
		positions = template.getPositions(startPos, direction);
		
		if(instaBuild.isChecked() && positions.size() <= 64)
			buildInstantly();
		else
			status = Status.BUILDING;
	}
	
	private void buildInstantly()
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		double rangeSq = Math.pow(range.getValue(), 2);
		
		for(BlockPos pos : positions)
		{
			if(!BlockUtils.getState(pos).getMaterial().isReplaceable())
				continue;
			
			Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
			
			for(Direction side : Direction.values())
			{
				BlockPos neighbor = pos.offset(side);
				
				// check if neighbor can be right-clicked
				if(!BlockUtils.canBeClicked(neighbor))
					continue;
				
				Vec3d sideVec = new Vec3d(side.getVector());
				Vec3d hitVec = posVec.add(sideVec.multiply(0.5));
				
				// check if hitVec is within range
				if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
					continue;
				
				// place block
				im.rightClickBlock(neighbor, side.getOpposite(), hitVec);
				
				break;
			}
		}
		
		positions.clear();
	}
	
	private void buildNormally()
	{
		BlockPos pos = getNextPos();
		
		// stop if done
		if(pos == null)
		{
			status = Status.IDLE;
			return;
		}
		
		// wait for right click timer
		if(IMC.getItemUseCooldown() > 0)
			return;
		
		placeBlockLegit(pos);
	}
	
	private BlockPos getNextPos()
	{
		for(Iterator itr = positions.iterator();;)
		{
			if(!itr.hasNext())
				return null;
			
			BlockPos pos = (BlockPos)itr.next();
			
			// remove already placed blocks
			if(!BlockUtils.getState(pos).getMaterial().isReplaceable())
			{
				itr.remove();
				continue;
			}
			
			return pos;
		}
	}
	
	private boolean placeBlockLegit(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		double rangeSq = Math.pow(range.getValue(), 2);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3d dirVec = new Vec3d(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));
			
			// check if hitVec is within range
			if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;
			
			// check line of sight
			if(MC.world
				.rayTrace(new RayTraceContext(eyesPos, hitVec,
					RayTraceContext.ShapeType.COLLIDER,
					RayTraceContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			// face block
			Rotation rotation = RotationUtils.getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookOnly packet =
				new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
					rotation.getPitch(), MC.player.onGround);
			MC.player.networkHandler.sendPacket(packet);
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			MC.player.swingHand(Hand.MAIN_HAND);
			IMC.setItemUseCooldown(4);
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		// scale and offset
		double scale = 1D * 7D / 8D;
		double offset = (1D - scale) / 2D;
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		int blocksDrawn = 0;
		for(Iterator<BlockPos> itr = positions.iterator(); itr.hasNext()
			&& blocksDrawn < 1024;)
		{
			BlockPos pos = itr.next();
			
			if(!BlockUtils.getState(pos).getMaterial().isReplaceable())
				continue;
			
			if(blocksDrawn == 0)
				drawGreenBox(pos, scale, offset);
			else
				drawOutline(pos, scale, offset);
			
			blocksDrawn++;
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glColor4f(1, 1, 1, 1);
	}
	
	private void drawGreenBox(BlockPos pos, double scale, double offset)
	{
		GL11.glPushMatrix();
		GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
		GL11.glTranslated(offset, offset, offset);
		GL11.glScaled(scale, scale, scale);
		
		GL11.glDepthMask(false);
		GL11.glColor4f(0F, 1F, 0F, 0.15F);
		RenderUtils.drawSolidBox();
		GL11.glDepthMask(true);
		
		GL11.glColor4f(0F, 0F, 0F, 0.5F);
		RenderUtils.drawOutlinedBox();
		
		GL11.glPopMatrix();
	}
	
	private void drawOutline(BlockPos pos, double scale, double offset)
	{
		GL11.glPushMatrix();
		GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
		GL11.glTranslated(offset, offset, offset);
		GL11.glScaled(scale, scale, scale);
		
		RenderUtils.drawOutlinedBox();
		
		GL11.glPopMatrix();
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE,
		BUILDING;
	}
}
