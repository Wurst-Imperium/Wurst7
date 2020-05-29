package net.wurstclient.waypoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.WaypointComponent;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.Map;

public class Waypoint implements Comparable<Waypoint> {
	
	private String name;
	private Vec3d position;
	private MinecraftClient mc = MinecraftClient.getInstance();
	
	public Waypoint(String name, Vec3d pos) {
		this.name = name;
		position = pos;
	}
	
	public String getName() {
		return name;
	}

	
	public Vec3d getPos() {
		return this.position;
	}

	public double getX() {
		return position.x;
	}

	public double getY() {
		return position.y;
	}

	public double getZ() {
		return position.z;
	}
	
	public String toString() {
		return this.name + ": {" + (int)position.x + ", " + (int)position.y + ", " + (int)position.z + "}";
	}

	@Override
	public int compareTo(Waypoint o) {
		return this.getName().compareToIgnoreCase(o.getName());
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		JsonArray coords = JsonUtils.GSON.toJsonTree(toArray()).getAsJsonArray();
		json.add(this.getName(), coords);
		return json;
	}

	private double[] toArray() {
		return new double[] {position.x, position.y, position.z};
	}

	public static Waypoint fromJson(JsonObject o) throws JsonException {
		for(Map.Entry<String, JsonElement> m : o.entrySet()) {
				if(!m.getValue().isJsonArray()) {
					throw new JsonException();
				}
				double[] arr = JsonUtils.GSON.fromJson(m.getValue(), double[].class);
				return new Waypoint(m.getKey(), new Vec3d(arr[0], arr[1], arr[2]));
		}
		return null;
	}

	public Component getComponent() {
		return new WaypointComponent(this);
	}
	

}
