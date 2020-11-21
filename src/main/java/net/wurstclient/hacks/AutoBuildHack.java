/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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
	private final FileSetting templateSetting = new FileSetting("Template",
		"Determines what to build.\n\n"
			+ "Templates are just JSON files. Feel free to\n"
			+ "add your own or to edit / delete the\n"
			+ "default templates.\n\n" + "If you mess up, simply press the\n"
			+ "'Reset to Defaults' button or\n" + "delete the folder.",
		"autobuild", folder -> DefaultAutoBuildTemplates.createFiles(folder));
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far to reach when placing blocks.\n" + "Recommended values:\n"
			+ "6.0 for vanilla\n" + "4.25 for NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS =
		new CheckboxSetting("Check line of sight",
			"Makes sure that you don't reach through walls\n"
				+ "when placing blocks. Can help with AntiCheat\n"
				+ "plugins but slows down building.",
			false);
	
	private final CheckboxSetting instaBuild = new CheckboxSetting("InstaBuild",
		"Builds small templates (<= 64 blocks) instantly.\n"
			+ "For best results, stand close to the block you're placing.",
		true);
	
	private final CheckboxSetting fastPlace = new CheckboxSetting(
		"Always FastPlace",
		"Builds as if FastPlace was enabled,\n" + "even if it's not.", true);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashSet<BlockPos> remainingBlocks = new LinkedHashSet<>();
	
	public AutoBuildHack()
	{
		super("AutoBuild", "Builds things automatically.\n"
			+ "Place a single block to start building.");
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
		
		remainingBlocks.clear();
		
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
	
	private void buildNormally()
	{
		updateRemainingBlocks();
		
		if(remainingBlocks.isEmpty())
		{
			status = Status.IDLE;
			return;
		}
		
		if(!fastPlace.isChecked() && IMC.getItemUseCooldown() > 0)
			return;
		
		placeNextBlock();
	}
	
	private void updateRemainingBlocks()
	{
		for(Iterator<BlockPos> itr = remainingBlocks.iterator(); itr.hasNext();)
		{
			BlockPos pos = itr.next();
			BlockState state = BlockUtils.getState(pos);
			
			if(!state.getMaterial().isReplaceable())
				itr.remove();
		}
	}
	
	private void placeNextBlock()
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = Math.pow(range.getValue(), 2);
		
		for(BlockPos pos : remainingBlocks)
			if(tryToPlace(pos, eyesPos, rangeSq))
				break;
	}
	
	private boolean tryToPlace(BlockPos pos, Vec3d eyesPos, double rangeSq)
	{
		Vec3d posVec = Vec3d.ofCenter(pos);
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor)
				|| BlockUtils.getState(neighbor).getMaterial().isReplaceable())
				continue;
			
			Vec3d dirVec = Vec3d.of(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));
			
			// check if hitVec is within range
			if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
				continue;
			
			// check if side is visible (facing away from player)
			if(distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;
			
			// check line of sight
			if(checkLOS.isChecked() && MC.world
				.raycast(new RaycastContext(eyesPos, hitVec,
					RaycastContext.ShapeType.COLLIDER,
					RaycastContext.FluidHandling.NONE, MC.player))
				.getType() != HitResult.Type.MISS)
				continue;
			
			// face block
			Rotation rotation = RotationUtils.getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookOnly packet =
				new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(),
					rotation.getPitch(), MC.player.isOnGround());
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
		remainingBlocks = template.getPositions(startPos, direction);
		
		if(instaBuild.isChecked() && template.size() <= 64)
			buildInstantly();
		else
			status = Status.BUILDING;
	}
	
	private void buildInstantly()
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		IClientPlayerInteractionManager im = IMC.getInteractionManager();
		double rangeSq = Math.pow(range.getValue(), 2);
		
		for(BlockPos pos : remainingBlocks)
		{
			if(!BlockUtils.getState(pos).getMaterial().isReplaceable())
				continue;
			
			Vec3d posVec = Vec3d.ofCenter(pos);
			
			for(Direction side : Direction.values())
			{
				BlockPos neighbor = pos.offset(side);
				
				// check if neighbor can be right-clicked
				if(!BlockUtils.canBeClicked(neighbor))
					continue;
				
				Vec3d sideVec = Vec3d.of(side.getVector());
				Vec3d hitVec = posVec.add(sideVec.multiply(0.5));
				
				// check if hitVec is within range
				if(eyesPos.squaredDistanceTo(hitVec) > rangeSq)
					continue;
				
				// place block
				im.rightClickBlock(neighbor, side.getOpposite(), hitVec);
				
				break;
			}
		}
		
		remainingBlocks.clear();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		double scale = 1D * 7D / 8D;
		double offset = (1D - scale) / 2D;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = Math.pow(range.getValue(), 2);
		
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2F);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor4f(0F, 0F, 0F, 0.5F);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		int blocksDrawn = 0;
		for(Iterator<BlockPos> itr = remainingBlocks.iterator(); itr.hasNext()
			&& blocksDrawn < 1024;)
		{
			BlockPos pos = itr.next();
			if(!BlockUtils.getState(pos).getMaterial().isReplaceable())
				continue;
			
			GL11.glPushMatrix();
			GL11.glTranslated(pos.getX(), pos.getY(), pos.getZ());
			GL11.glTranslated(offset, offset, offset);
			GL11.glScaled(scale, scale, scale);
			
			Vec3d posVec = Vec3d.ofCenter(pos);
			
			if(eyesPos.squaredDistanceTo(posVec) <= rangeSq)
				drawGreenBox();
			else
				RenderUtils.drawOutlinedBox();
			
			GL11.glPopMatrix();
			blocksDrawn++;
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
		GL11.glColor4f(1, 1, 1, 1);
	}
	
	private void drawGreenBox()
	{
		GL11.glDepthMask(false);
		GL11.glColor4f(0F, 1F, 0F, 0.15F);
		RenderUtils.drawSolidBox();
		GL11.glDepthMask(true);
		
		GL11.glColor4f(0F, 0F, 0F, 0.5F);
		RenderUtils.drawOutlinedBox();
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE,
		BUILDING;
	}
}
