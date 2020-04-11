package net.wurstclient.hacks;

import net.wurstclient.events.CameraTransformViewBobbingListener;
import net.wurstclient.events.RenderListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.util.RotationUtils;

public class BlockEspHack extends Hack implements UpdateListener, CameraTransformViewBobbingListener, RenderListener {

	private final EnumSetting<Style> style = new EnumSetting<Style>("Style", Style.values(), Style.BOXES);
	private final BlockListSetting targetBlocks = new BlockListSetting("Target blocks", "Blocks to highlight");

	private List<String> blockNames;
	private final ArrayList<Box> matchingBlocks = new ArrayList<Box>();
	private ArrayList<Box> renderBoxes = new ArrayList<Box>();

	private int espBox;

	public BlockEspHack() {
		super("BlockESP", "Highight selected blocks.");
		setCategory(Category.RENDER);
		addSetting(style);
		addSetting(targetBlocks);
	}

	@Override
	public void onEnable() {
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(CameraTransformViewBobbingListener.class, this);
		EVENTS.add(RenderListener.class, this);
		setupDisplayLists();
	}

	private void setupDisplayLists()
	{
		Box box = new Box(BlockPos.ORIGIN);
		
		espBox = GL11.glGenLists(1);
		GL11.glNewList(espBox, GL11.GL_COMPILE);
		GL11.glColor4f(0.4F, 0.6F, 0.4F, 0.25F);
		RenderUtils.drawSolidBox(box);
		GL11.glColor4f(0.4F, 0.6F, 0.4F, 0.5F);
		RenderUtils.drawOutlinedBox(box);
		GL11.glEndList();
	}

	@Override
	public void onDisable() {
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(CameraTransformViewBobbingListener.class, this);
		EVENTS.remove(RenderListener.class, this);

		GL11.glDeleteLists(espBox, 1);
	}

	@Override
	public void onRender(float partialTicks) {
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(2);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();

		if(renderBoxes.size()>0)
			if(style.getSelected().boxes)
				boxRenderer(renderBoxes, espBox);
			if(style.getSelected().lines) {
				Vec3d start = RotationUtils.getClientLookVec()
					.add(RenderUtils.getCameraPos());
			
				GL11.glBegin(GL11.GL_LINES);
			
				GL11.glColor4f(0.4F, 0.6F, 0.4F, 1);
				lineRenderer(start, renderBoxes);
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

	private void boxRenderer(ArrayList<Box> boxes, int displayList)
	{
		
		for(Box box : boxes)
		{
			GL11.glPushMatrix();
			GL11.glTranslated(box.x1, box.y1, box.z1);
			GL11.glScaled(box.x2 - box.x1, box.y2 - box.y1, box.z2 - box.z1);
			GL11.glCallList(displayList);
			GL11.glPopMatrix();
		}
	}
	
	private void lineRenderer(Vec3d start, ArrayList<Box> boxes)
	{
		for(Box box : boxes)
		{
			Vec3d end = box.getCenter();
			GL11.glVertex3d(start.x, start.y, start.z);
			GL11.glVertex3d(end.x, end.y, end.z);
		}
	}

	@Override
	public void onUpdate() {
		BlockPos playerPos = new BlockPos(MC.player.getX(), 0, MC.player.getZ());
		blockNames = targetBlocks.getBlockNames();

		int modulo = MC.player.age % 64;

		int startY = 255 - modulo * 4;
		int endY = startY - 4;

		for(int y = startY; y > endY; y--) {
			for(int x = 64; x > -64; x--) {
				for(int z = 64; z > -64; z--)
				{
					BlockPos pos = playerPos.add(x, y, z);
					if(Collections.binarySearch(blockNames,
						BlockUtils.getName(pos)) >= 0)
						matchingBlocks.add(BlockUtils.getBoundingBox(pos));
				}
			}
		}	
		if(modulo == 0) {
			renderBoxes.clear();
			for(Box box : matchingBlocks) {
				renderBoxes.add(box);
			}
			matchingBlocks.clear();
		}
	}

	@Override
	public void onCameraTransformViewBobbing(
		CameraTransformViewBobbingEvent event)
	{
		if(style.getSelected().lines)
			event.cancel();
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