/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EyeOfEnderEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.commands.GoToCmd;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

@SearchTags({"stronghold esp", "stronghold", "esp", "finder", "strongholdfinder", "stronghold finder"})
public class StrongholdFinderHack extends Hack implements UpdateListener, RenderListener {

	private boolean first = true;
	private double firstYaw = 0.0;
	private int waitAfter;
	private Vec3d firstPos;
	private GoToCmd gotocmd;
	private double strongholdz;
	private double strongholdx;
	
	private final SliderSetting accuracy = new SliderSetting("Distance to walk",
			"Causes more accuracy sometimes.", 100, 50,
			500, 1, ValueDisplay.INTEGER);
	
	private final EnumSetting<FindingDirection> direction = new EnumSetting<FindingDirection>("Direction to walk", "Causes more accuracy sometimes.",FindingDirection.values(), FindingDirection.N);
	
	public StrongholdFinderHack() {
		super("StrongholdFinder"); //Calculates the stronghold's position almost perfectly by walking
		setCategory(Category.RENDER);
		addSetting(accuracy);
		addSetting(direction);
	}
	
	private int getEnderEyeSlot()
	{
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();
		if (MC.player.getMainHandStack().getItem() instanceof EnderEyeItem)
			return inventory.selectedSlot;
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getStack(slot);
			
			if(!(stack.getItem() instanceof EnderEyeItem))
				continue;
			
			return slot;
		}
		return -1;
	}
	
	@Override
	public void onEnable() {
		gotocmd = WURST.getCmds().goToCmd;
		strongholdx = 0;
		strongholdz = 0;
		waitAfter = 0;
		first = true;
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
	}

	@Override
	public void onUpdate() {
		List<Entity> entity = StreamSupport.stream(MC.world.getEntities().spliterator() ,false).filter(e -> (e instanceof EyeOfEnderEntity)).sorted(Comparator.<Entity>comparingDouble(pos -> pos.getPos().squaredDistanceTo(MC.player.getPos()))).limit(1).toList();
		Entity pearl;
		if (!entity.isEmpty() && (pearl = entity.get(0)).getVelocity().y < 0) {
			double yaw = Math.atan2(pearl.getX() - MC.player.getX(), pearl.getZ() - MC.player.getZ());
			//ChatUtils.message(String.valueOf(-Math.toDegrees(yaw)));
			if (first) {
				firstYaw = Math.tan(yaw);
				firstPos = MC.player.getPos(); 
				ChatUtils.message("First position set!");
				try {gotocmd.call(direction.getSelected().getCordsToGoto(accuracy.getValueI())); } catch (CmdException e1) {}
				first = false;
			}
			else if(waitAfter >= 10) {
				//                 (    yaw      *                  -z                 +                  x                 ) / (firstYaw -     yaw      )
				this.strongholdz = (Math.tan(yaw)* (firstPos.z - MC.player.getPos().z) + (MC.player.getPos().x - firstPos.x)) / (firstYaw - Math.tan(yaw));
				this.strongholdx = firstYaw * strongholdz;
				strongholdz += firstPos.z; strongholdx += firstPos.x;
				ChatUtils.message("Done! Stronghold cords:");
				ChatUtils.message("X:"+ strongholdx);
				ChatUtils.message("Z:"+ strongholdz);
				EVENTS.add(RenderListener.class, this);
				EVENTS.remove(UpdateListener.class, this);
				first = true;
				return;
			}
		} else if(entity.isEmpty() && ((!gotocmd.isActive() && waitAfter >= 10) || first)) {
			int slot = getEnderEyeSlot();
			if (slot != -1) {
				MC.player.getInventory().selectedSlot = slot;
				IMC.getInteractionManager().rightClickItem();
			} else {
				ChatUtils.error("There is no eye of ender item in your hotbar!");
				setEnabled(false);
			}
		} else if(!gotocmd.isActive() && !first && waitAfter < 10) {
			waitAfter++;
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks) {
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		matrixStack.push();
		RenderUtils.applyRenderOffset(matrixStack);
		
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
		
		// box
		matrixStack.push();
		matrixStack.translate(strongholdx, 0, strongholdz);
		RenderUtils.drawOutlinedBox(matrixStack);
		
		RenderSystem.setShaderColor(1F, 1F, 1F, 0.5F);
		RenderUtils.drawSolidBox(matrixStack);
		matrixStack.pop();
		
		// line
		Vec3d start =
			RotationUtils.getClientLookVec().add(RenderUtils.getCameraPos());
		Vec3d end = new Vec3d(strongholdx+0.5, 0, strongholdz+0.5);
		
		Matrix4f matrix = matrixStack.peek().getPositionMatrix();
		Tessellator tessellator = RenderSystem.renderThreadTesselator();
		BufferBuilder bufferBuilder = tessellator.getBuffer();
		RenderSystem.setShader(GameRenderer::getPositionShader);
		
		bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES,
			VertexFormats.POSITION);
		bufferBuilder
			.vertex(matrix, (float)start.x, (float)start.y, (float)start.z)
			.next();
		bufferBuilder.vertex(matrix, (float)end.x, (float)end.y, (float)end.z)
			.next();
		tessellator.draw();
		
		matrixStack.pop();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private enum FindingDirection{
		N("-Z", new Vec3i(0,0,-1)), //North
		W("-X", new Vec3i(-1,0,0)), //West
		S("+Z", new Vec3i(0,0,1)), //South
		E("+X", new Vec3i(1,0,0)), //East
		NW("-Z and +X", new Vec3i(1,0,-1)), //North-West
		SW("+Z and +X", new Vec3i(1,0,1)), //South-West
		NE("-Z and -X", new Vec3i(-1,0,-1)), //North-East
		SE("+Z and -X", new Vec3i(-1,0,1)); //South-East
		
		private String name;
		private Vec3i direction;
		private FindingDirection(String name, Vec3i direction) {
			this.name = name;
			this.direction = direction;
		}
		
		@Nullable
		public BlockPos getNearestBlock(int x, int z) 
		{
			List<BlockPos> blocks = BlockUtils.getAllInBoxStream(new BlockPos(x, 0, z), new BlockPos(x, 255, z))
			.filter(b -> !BlockUtils.getState(b.down()).isAir() && BlockUtils.getState(b).isAir() && BlockUtils.getState(b.up()).isAir())
			.sorted(Comparator.<BlockPos>comparingDouble(pos -> Math.abs(pos.getY() - MC.player.getY()))).limit(1).toList();
			
			if (blocks.size() == 0) {return null;}
			
			return blocks.get(0);
		}
		
		public String[] getCordsToGoto(Integer distance) {
			BlockPos pos = this.getNearestBlock(MC.player.getBlockPos().getX()+distance*this.direction.getX(), MC.player.getBlockPos().getZ()+distance*this.direction.getZ());
			if (pos == null) { ChatUtils.error("Position out of distance."); pos = MC.player.getBlockPos();}
			return new String[] {String.valueOf(pos.getX()), String.valueOf(pos.getY()), String.valueOf(pos.getZ())};
			
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		
	}
}
