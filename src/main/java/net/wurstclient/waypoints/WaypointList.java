package net.wurstclient.waypoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.registry.RegistryKey;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

public class WaypointList {
    private boolean disableSaving = false;

    private Path path;

    private TreeMap<String, List<Waypoint>> wpLists = new TreeMap<>();

    public WaypointList(Path path) {
        this.path = path;
    }

    public void addAndSave(String worldId, Waypoint point) {
        wpLists.get(worldId).add(point);
        save();
    }

    public void removeAndSave(String worldId, Waypoint point) {
        wpLists.get(worldId).remove(point);
    }

    public TreeMap<String, List<Waypoint>> getWpLists() {
        return this.wpLists;
    }



    public void load() {
        try {
            wpLists.clear();
            JsonObject json = JsonUtils.parseFileToObject(path).toJsonObject();
            load(json);
        }catch(NoSuchFileException e) {}
        catch(IOException | JsonException e) {
            System.out.println("Couldn't load " + path.getFileName());
            e.printStackTrace();
        }

        save();

    }

    public void load(JsonObject json) throws JsonException {
        try {
            disableSaving = true;

            for(Entry<String, JsonElement> e : json.entrySet()) {
                wpLists.put(e.getKey(), toList(e.getValue()));
            }
        }finally {
            disableSaving = false;
        }
    }

    private List<Waypoint> toList(JsonElement json) throws JsonException {
        List<Waypoint> ret = new ArrayList<>();
        if(!json.isJsonArray()) {
            throw new JsonException();
        }
        JsonArray arr = json.getAsJsonArray();
        for(int i=0; i<arr.size(); i++)
            ret.add(Waypoint.fromJson(arr.get(i).getAsJsonObject()));

        return ret;
    }

    public void save() {
        if(disableSaving)
            return;

        JsonObject json = createJson();

        try {
            JsonUtils.toJson(json, path);
        }catch(IOException | JsonException e) {
            System.out.println("Couldn't save " + path.getFileName());
            e.printStackTrace();
        }
    }

    public JsonObject createJson() {
        JsonObject json = new JsonObject();
        for(Entry<String, List<Waypoint>> e : wpLists.entrySet()) {
            JsonArray array = new JsonArray();
            for(Waypoint w : e.getValue()) {
                array.add(w.toJson());
            }
            json.add(e.getKey(), array);
        }
        return json;
    }



    public static String toWorldId(String serverName, RegistryKey<DimensionType> type) {
        return serverName + "::" + type.getValue();
    }

    public static String[] fromWorldId(String worldId) {
        return worldId.split("::");
    }



}
