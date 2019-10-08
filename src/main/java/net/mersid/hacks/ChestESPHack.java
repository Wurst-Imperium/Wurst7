package net.mersid.hacks;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.mersid.util.GetEntitiesInRadius;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.wurstclient.Category;
import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public class ChestESPHack extends Hack implements UpdateListener,
	CameraTransformViewBobbingListener, RenderListener
{
	
	private final EnumSetting<Style> style =
		new EnumSetting<>("Style", Style.values(), Style.BOXES);
	
	private final ArrayList<Box> basicChests = new ArrayList<>();
	private final ArrayList<Box> trappedChests = new ArrayList<>();
	private final ArrayList<Box> enderChests = new ArrayList<>();
	private final ArrayList<Entity> minecarts = new ArrayList<>();
	
	private int greenBox;
	private int orangeBox;
	private int cyanBox;
	private int normalChests;
	
	private boolean init = false;
	
	public ChestESPHack()
	{
		super("ChestESP",
			"Highlights nearby chests.\n"
				+ "\u00a7agreen\u00a7r - normal chests\n"
				+ "\u00a76orange\u00a7r - trapped chests\n"
				+ "\u00a7bcyan\u00a7r - ender chests");
		setCategory(Category.RENDER);
		addSetting(style);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		
		if(MC.player != null)
			GLInit();
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
		GL11.glDeleteLists(greenBox, 1);
		greenBox = 0;
		GL11.glDeleteLists(orangeBox, 1);
		orangeBox = 0;
		GL11.glDeleteLists(cyanBox, 1);
		cyanBox = 0;
		GL11.glDeleteLists(normalChests, 1);
		normalChests = 0;
	}
	
	private void GLInit()
	{
		Box bb = new Box(BlockPos.ORIGIN);
		
		greenBox = GL11.glGenLists(1);
		GL11.glNewList(greenBox, GL11.GL_COMPILE);
		GL11.glColor4f(0, 1, 0, 0.25F);
		RenderUtils.drawSolidBox(bb);
		GL11.glColor4f(0, 1, 0, 0.5F);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
		
		orangeBox = GL11.glGenLists(1);
		GL11.glNewList(orangeBox, GL11.GL_COMPILE);
		GL11.glColor4f(1, 0.5F, 0, 0.25F);
		RenderUtils.drawSolidBox(bb);
		GL11.glColor4f(1, 0.5F, 0, 0.5F);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
		
		cyanBox = GL11.glGenLists(1);
		GL11.glNewList(cyanBox, GL11.GL_COMPILE);
		GL11.glColor4f(0, 1, 1, 0.25F);
		RenderUtils.drawSolidBox(bb);
		GL11.glColor4f(0, 1, 1, 0.5F);
		RenderUtils.drawOutlinedBox(bb);
		GL11.glEndList();
		
		normalChests = GL11.glGenLists(1);
		init = true;
	}
	
	@Override
	public void onUpdate()
	{
		World world = MC.player.world;
		if(!init)
			GLInit();
		
		basicChests.clear();
		trappedChests.clear();
		enderChests.clear();
		
		for(BlockEntity tileEntity : world.blockEntities)
			if(tileEntity instanceof ChestBlockEntity)
			{
				/*
				 * ChestBlockEntity chest = (ChestBlockEntity)tileEntity;
				 * ChestType type =
				 * chest.getCachedState().get(ChestBlock.CHEST_TYPE); // If a
				 * chest is solo, left, or right.
				 * 
				 * // For double chests, AABB union will combine them. No need
				 * to render it again.
				 * // Picking right is an arbitrary decision. We could have
				 * picked left.
				 * if (type == ChestType.RIGHT) continue;
				 * 
				 * // get hitbox
				 * BlockPos pos = chest.getPos();
				 * Box bb = BlockUtils.getBoundingBox(pos);
				 * 
				 * // Kinda taken from ChestBlockEntity. Finds the adjacent
				 * chest for doubles.
				 * BlockState state = chest.getCachedState();
				 * BlockPos opos =
				 * chest.getPos().offset(ChestBlock.getFacing(state));
				 * BlockEntity ote = chest.getWorld().getBlockEntity(opos);
				 * 
				 * if (ote instanceof ChestBlockEntity)
				 * {
				 * // Separation is to ensure that normal and trapped chests do
				 * not union-box each other.
				 * //if (type == ChestType.SINGLE) return;
				 * 
				 * if ((ote instanceof TrappedChestBlockEntity && chest
				 * instanceof TrappedChestBlockEntity) ||
				 * !(ote instanceof TrappedChestBlockEntity) && !(chest
				 * instanceof TrappedChestBlockEntity)
				 * )
				 * {
				 * ChestType type2 =
				 * ote.getCachedState().get(ChestBlock.CHEST_TYPE);
				 * System.out.println(type);
				 * if (type == ChestType.SINGLE) break;
				 * 
				 * BlockPos pos2 = ote.getPos();
				 * Box bb2 = BlockUtils.getBoundingBox(pos2);
				 * bb = bb.union(bb2);
				 * }
				 * 
				 * }
				 */
				
				// Gets the chest
				ChestBlockEntity chest = (ChestBlockEntity)tileEntity;
				ChestType type =
					chest.getCachedState().get(ChestBlock.CHEST_TYPE);
				
				// get hitbox
				BlockPos pos = chest.getPos();
				Box bb = BlockUtils.getBoundingBox(pos);
				
				if(type == ChestType.SINGLE)
				{
					if(chest instanceof TrappedChestBlockEntity)
						trappedChests.add(bb);
					else
						basicChests.add(bb);
				}else
				{
					// Double chests. Ignore one half so that it does not get
					// double-rendered
					if(type == ChestType.RIGHT)
						continue;
					
					// Kinda taken from ChestBlockEntity. Finds the adjacent
					// chest for doubles.
					BlockState state = chest.getCachedState();
					BlockPos opos =
						chest.getPos().offset(ChestBlock.getFacing(state));
					BlockEntity ote = chest.getWorld().getBlockEntity(opos);
					
					if(ote instanceof ChestBlockEntity)
					{
						BlockPos pos2 = ote.getPos();
						Box bb2 = BlockUtils.getBoundingBox(pos2);
						bb = bb.union(bb2);
						
						if(chest instanceof TrappedChestBlockEntity)
							trappedChests.add(bb);
						else
							basicChests.add(bb);
					}
				}
			}else if(tileEntity instanceof EnderChestBlockEntity) // Some
																	// reason,
																	// ender
																	// chests
																	// are not
																	// ChestBlockEntity.
			{
				BlockPos pos = ((EnderChestBlockEntity)tileEntity).getPos();
				Box bb = BlockUtils.getBoundingBox(pos);
				enderChests.add(bb);
			}
		
		GL11.glNewList(normalChests, GL11.GL_COMPILE);
		renderBoxes(basicChests, greenBox);
		renderBoxes(trappedChests, orangeBox);
		renderBoxes(enderChests, cyanBox);
		GL11.glEndList();
		
		// minecarts
		minecarts.clear();
		for(Entity entity : GetEntitiesInRadius.get())
			if(entity instanceof ChestMinecartEntity)
				minecarts.add(entity);
			
	}
	
	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		
		GL11.glPushMatrix();
		GL11.glTranslated(-BlockEntityRenderDispatcher.renderOffsetX,
			-BlockEntityRenderDispatcher.renderOffsetY,
			-BlockEntityRenderDispatcher.renderOffsetZ);
		
		// minecart interpolation
		ArrayList<Box> minecartBoxes = new ArrayList<>(minecarts.size());
		minecarts.forEach(e -> {
			double offsetX =
				-(e.x - e.prevRenderX) + (e.x - e.prevRenderX) * partialTicks;
			double offsetY =
				-(e.y - e.prevRenderY) + (e.y - e.prevRenderY) * partialTicks;
			double offsetZ =
				-(e.z - e.prevRenderZ) + (e.z - e.prevRenderZ) * partialTicks;
			minecartBoxes
				.add(e.getBoundingBox().offset(offsetX, offsetY, offsetZ));
		});
		
		if(style.getSelected().boxes)
		{
			GL11.glCallList(normalChests);
			renderBoxes(minecartBoxes, greenBox);
		}
		
		if(style.getSelected().lines)
		{
			// This block of code down to the end of else statement copied from
			// EntityESP.
			Vec3d start = new Vec3d(BlockEntityRenderDispatcher.renderOffsetX,
				BlockEntityRenderDispatcher.renderOffsetY,
				BlockEntityRenderDispatcher.renderOffsetZ);
			
			start = start.add(RotationUtils.getClientLookVec());
			
			GL11.glBegin(GL11.GL_LINES);
			
			GL11.glColor4f(0, 1, 0, 0.5F);
			renderLines(start, basicChests);
			renderLines(start, minecartBoxes);
			
			GL11.glColor4f(1, 0.5F, 0, 0.5F);
			renderLines(start, trappedChests);
			
			GL11.glColor4f(0, 1, 1, 0.5F);
			renderLines(start, enderChests);
			
			GL11.glEnd();
		}
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	private void renderBoxes(ArrayList<Box> boxes, int displayList)
	{// GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		for(Box bb : boxes)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(bb.minX, bb.minY, bb.minZ);
			GL11.glScaled(bb.maxX - bb.minX, bb.maxY - bb.minY,
				bb.maxZ - bb.minZ);
			GL11.glCallList(displayList);
			GL11.glPopMatrix();
			
		}// GL11.glDisable(GL11.GL_DEPTH_TEST);
		
	}
	
	private void renderLines(Vec3d start, ArrayList<Box> boxes)
	{// GL11.glEnable(GL11.GL_DEPTH_TEST);
		for(Box bb : boxes)
		{
			Vec3d end = bb.getCenter();
			
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}// GL11.glDisable(GL11.GL_DEPTH_TEST);
	}
	
	private enum Style
	{
		BOXES("Boxes only", true, false),
		LINES("Lines only", false, true),
		LINES_AND_BOXES("Lines and boxes", true, true);
		
		private final String name;
		private final boolean boxes;
		private final boolean lines;
		
		private Style(String name, boolean boxes, boolean lines)
		{
			this.name = name;
			this.boxes = boxes;
			this.lines = lines;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
