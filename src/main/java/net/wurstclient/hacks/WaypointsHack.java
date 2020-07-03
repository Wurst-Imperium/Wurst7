package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.World;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.WaypointWindow;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.waypoints.WaypointNameScreen;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.RenderUtils;
import net.wurstclient.waypoints.WaypointList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.hack.Hack;
import net.wurstclient.waypoints.Waypoint;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_BLEND;

@SearchTags({"waypoints", "waypoint"})
public class WaypointsHack extends Hack implements UpdateListener, RenderListener {
	private List<Waypoint> activeWaypoints;
	public Waypoint selectedWaypoint;
	private World current;
	private boolean openFlag;
	private final boolean voxelMap = FabricLoader.getInstance().isModLoaded("voxelmap");

	private SliderSetting diamondR = new SliderSetting("R", 1, 0, 1, 0.1, SliderSetting.ValueDisplay.DECIMAL);
	private SliderSetting diamondG = new SliderSetting("G", 0, 0, 1, 0.1, SliderSetting.ValueDisplay.DECIMAL);
	private SliderSetting diamondB = new SliderSetting("B", 1, 0, 1, 0.1, SliderSetting.ValueDisplay.DECIMAL);
	
	public WaypointsHack() {
		super("Waypoints", "Draws selected waypoint.");
		setCategory(Category.RENDER);
		addSetting(diamondR);
		addSetting(diamondG);
		addSetting(diamondB);
		EVENTS.add(UpdateListener.class, this);
	}

	@Override
	public void onEnable() {
		if(voxelMap) {
			setEnabled(false);
			return;
		}
		EVENTS.add(RenderListener.class, this);
		updateSelected();
	}

	@Override
	public void onDisable() {
		EVENTS.remove(RenderListener.class, this);
	}

	@Override
	public void onUpdate() {
		if(openFlag) {
			openWaypointScreen();
			openFlag = false;
		}
		if(MC.world == null || (MC.world == current && MC.world.getDimension() == current.getDimension()))
			return;
		assert MC.player != null;
		current = MC.player.world;
		WURST.getWaypointList().getWpLists().computeIfAbsent(getWorldId(), k -> new ArrayList<>());
		activeWaypoints = WURST.getWaypointList().getWpLists().get(getWorldId());
		repopulateWindow();
		selectedWaypoint = null;
		updateSelected();
	}

	public void repopulateWindow() {
		ArrayList<Component> list = new ArrayList<>();
		activeWaypoints.forEach(w -> list.add(w.getComponent()));
		WaypointWindow.getWindow().repopulate(list);
	}

	public void updateSelected() {
		if(!activeWaypoints.isEmpty() && this.isEnabled()) {
			if(selectedWaypoint==null)
				selectedWaypoint = activeWaypoints.get(0);
		}
	}

	public String getWorldId() {
		String ret = "";
		if(MC.isInSingleplayer())
			ret += WaypointList.toWorldId("singleplayer", current.getDimensionRegistryKey());
		else
			ret += WaypointList.toWorldId(MC.getCurrentServerEntry().address, current.getDimensionRegistryKey());

		return ret;
	}

	public List<Waypoint> getActiveWaypoints() {
		return activeWaypoints;
	}

	public void selectWaypointUp() {
		if(activeWaypoints.isEmpty())
			return;
		if(activeWaypoints.indexOf(selectedWaypoint) == 0) {
			selectedWaypoint = activeWaypoints.get(activeWaypoints.size()-1);
			return;
		}
		selectedWaypoint = activeWaypoints.get(activeWaypoints.indexOf(selectedWaypoint) -1);
	}

	public void selectWaypointDown() {
		if(activeWaypoints.isEmpty())
			return;
		if(activeWaypoints.indexOf(selectedWaypoint) == activeWaypoints.size()-1) {
			selectedWaypoint = activeWaypoints.get(0);
			return;
		}
		selectedWaypoint = activeWaypoints.get(activeWaypoints.indexOf(selectedWaypoint)+1);
	}


	public void openWaypointScreen() {
		MC.openScreen(new WaypointNameScreen(MC.currentScreen, this));
	}

	public boolean addWaypoint(String name, double x, double y, double z)
	{
		return addWaypoint(name, new Vec3d(x, y, z));
	}

	public boolean addWaypoint(String name, Vec3d pos)
	{
		if(name.isEmpty()) {
			int waypointCounter = 1;
			boolean found;
			while(true) {
				found = false;
				for(Waypoint point: activeWaypoints) {
					if(point.getName().equals("Waypoint " + waypointCounter)) {
						found = true;
						break;
					}
				}
				if(!found) break;
				waypointCounter++;
			}
			addWaypoint(new Waypoint("Waypoint " + waypointCounter, pos));
		}
		else {
			for(Waypoint point : activeWaypoints) {
				if(point.getName().equals(name)) {
					return false;
				}
			}
			addWaypoint(new Waypoint(name, pos));
		}
		return true;
	}

	public void addWaypoint(Waypoint wp)
	{
		activeWaypoints.add(wp);
		activeWaypoints.sort(Comparator.naturalOrder());
		WURST.getWaypointList().save();
		ChatUtils.message("Waypoint added.");
		WaypointWindow.getWindow().add(wp.getComponent());
		updateSelected();
	}

	public boolean removeWaypoint(String name)
	{
		for(Waypoint point : activeWaypoints)
		{
			if(point.getName().equals(name))
				return removeWaypoint(point);
		}
		return false;
	}
	
	public boolean removeWaypoint() {
		for(Waypoint point : activeWaypoints) {
			if(point.getPos().distanceTo(MC.player.getPos()) <= 5)
				return removeWaypoint(point);
		}
		return false;
	}

	public boolean removeWaypoint(Waypoint point) {
		if(!activeWaypoints.contains(point))
			return false;
		if(point == selectedWaypoint) {
			if(activeWaypoints.indexOf(selectedWaypoint)==0)
				selectWaypointDown();
			else
				selectWaypointUp();
		}
		activeWaypoints.remove(point);
		activeWaypoints.sort(Comparator.naturalOrder());
		WURST.getWaypointList().save();
		repopulateWindow();
		if(activeWaypoints.isEmpty())
			selectedWaypoint = null;
		ChatUtils.message("Waypoint removed.");
		return true;

	}

	public void removeAllWaypoints()
	{
		activeWaypoints.clear();
		WURST.getWaypointList().save();
		repopulateWindow();
		selectedWaypoint = null;
		ChatUtils.message("All waypoints on active world removed.");
	}

	@Override
	public void onRender(float partialTicks) {
		if(selectedWaypoint == null)
			return;
		//GL Enable
		GL11.glEnable(GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_LINE_SMOOTH);
		GL11.glLineWidth(3);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);

		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		renderSelectedWaypoint(partialTicks);

		GL11.glPopMatrix();

		//GL Reset
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LINE_SMOOTH);
	}
	
	@Override
	public boolean processCmd(String cmd) {
		switch(cmd) {
			case "addwaypoint":
				openFlag = !voxelMap;
				return true;
			case "delwaypoint":
				if(!voxelMap)
					removeWaypoint();
				return true;
			case "wpselectup":
				if(!voxelMap)
					selectWaypointUp();
				return true;
			case "wpselectdown":
				if(!voxelMap)
					selectWaypointDown();
				return true;
			default:
				return false;
		}
	}

	public void renderSelectedWaypoint(float partialTicks) {
		Vec3d start;
		boolean drawLine = false;
		double x = MC.player.prevX + (MC.player.getX() - MC.player.prevX) * partialTicks;
		double y = MC.player.prevY + (MC.player.getY() - MC.player.prevY) * partialTicks;
		double z = MC.player.prevZ + (MC.player.getZ() - MC.player.prevZ) * partialTicks;
		Vec3d actualPos = new Vec3d(x,y,z);
		if(selectedWaypoint.getPos().distanceTo(MC.player.getPos()) <= 30) {
			start = selectedWaypoint.getPos();
			drawLine = true;
		}
		else {
			start = RotationUtils.getPointAtDistanceTo(actualPos, selectedWaypoint.getPos(), 20);
		}
		double scale =actualPos.distanceTo(start);
		scale = Math.max(scale * 0.03, 0.1);
		Vec3d end = new Vec3d(start.getX(), start.getY() + 1.5, start.getZ());
		GL11.glColor4d(diamondR.getValue(),diamondG.getValue(),diamondB.getValue(), 0.4f);
		RenderUtils.drawDiamond(end, scale);
		if(!drawLine)
			return;
		GL11.glBegin(GL11.GL_LINES);
		GL11.glColor4d(1,1,1,0.2f);
		GL11.glVertex3d(start.x, start.y, start.z);
		GL11.glVertex3d(end.x, end.y-(scale*2) , end.z);
		GL11.glEnd();
	}
}
