/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.keybinds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import net.wurstclient.WurstClient;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.profiles.ManagedProfile;

public final class KeybindManaged extends ManagedProfile {
    public static final Set<Keybind> DEFAULT_KEYBINDS = createDefaultKeybinds();
    private final ArrayList<Keybind> keybinds = new ArrayList<>();

    private KeybindsFile currentKeybindsFile;

    public KeybindManaged(String defaultKeybindFile) {
        this.profilesFolder = WurstClient.INSTANCE.getWurstFolder().resolve("keybinds");
        this.currentKeybindsFile = new KeybindsFile(this.profilesFolder.resolve(defaultKeybindFile));
        this.loadProfileKeybinds();
    }

    public String getCommands(String key) {
        for (Keybind keybind : keybinds) {
            if (!key.equals(keybind.getKey()))
                continue;

            return keybind.getCommands();
        }

        return null;
    }

    public List<Keybind> getAllKeybinds() {
        return Collections.unmodifiableList(keybinds);
    }

    public void add(String key, String commands) {
        keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
        keybinds.add(new Keybind(key, commands));
        keybinds.sort(null);
        currentKeybindsFile.save(this.createJson());
    }

    private void setKeybinds(Set<Keybind> newKeybinds) {
        this.keybinds.clear();
        this.keybinds.addAll(newKeybinds);
        this.keybinds.sort(null);
        currentKeybindsFile.save(this.createJson());
    }

    public void loadProfileKeybinds() {
        setKeybinds(currentKeybindsFile.loadKeybinds());
        this.currentKeybindsFile.save(this.createJson());

    }

    public void setDefaultKeybinds() {
        setKeybinds(DEFAULT_KEYBINDS);
    }

    public void remove(String key) {
        keybinds.removeIf(keybind -> key.equals(keybind.getKey()));
        currentKeybindsFile.save(this.createJson());
    }

    public void removeAll() {
        keybinds.clear();
        currentKeybindsFile.save(this.createJson());
    }

    @Override
    public String getDisplayName() {
        return "Keybinds " + currentKeybindsFile.getBaseName();
    }

    @Override
    public void loadProfile(String fileName) throws IOException, JsonException {
        currentKeybindsFile = new KeybindsFile(profilesFolder.resolve(fileName));
        Set<Keybind> profileKeybinds = currentKeybindsFile.loadKeybinds();
        if (profileKeybinds.isEmpty()) {
            this.setDefaultKeybinds();
        }
        this.loadProfileKeybinds();
    }

    public void saveProfile(String fileName) throws IOException, JsonException {
        KeybindsFile pending = new KeybindsFile(profilesFolder.resolve(fileName));
        pending.save(this.createJson());
        currentKeybindsFile = pending;
    }

    public JsonObject createJson() {
        JsonObject json = new JsonObject();
        for (Keybind kb : this.getAllKeybinds())
            json.addProperty(kb.getKey(), kb.getCommands());
        return json;
    }

    @Override
    public int size() {
        return this.keybinds.size();
    }

    @Override
    public void setDefaults() {
        setDefaultKeybinds();
    }

    private static Set<Keybind> createDefaultKeybinds() {
        Set<Keybind> set = new LinkedHashSet<>();
        addKB(set, "b", "fastplace;fastbreak");
        addKB(set, "c", "fullbright");
        addKB(set, "g", "flight");
        addKB(set, "semicolon", "speednuker");
        addKB(set, "h", "say /home");
        addKB(set, "j", "jesus");
        addKB(set, "k", "multiaura");
        addKB(set, "n", "nuker");
        addKB(set, "r", "killaura");
        addKB(set, "right.shift", "navigator");
        addKB(set, "right.control", "clickgui");
        addKB(set, "u", "freecam");
        addKB(set, "x", "x-ray");
        addKB(set, "y", "sneak");
        return Collections.unmodifiableSet(set);
    }

    private static void addKB(Set<Keybind> set, String key, String cmds) {
        set.add(new Keybind("key.keyboard." + key, cmds));
    }
}
