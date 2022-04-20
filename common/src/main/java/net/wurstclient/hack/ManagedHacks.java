/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hack;

import com.google.gson.JsonObject;
import net.wurstclient.WurstClient;
import net.wurstclient.events.EnableHackListener;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.profiles.ManagedProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

public final class ManagedHacks extends ManagedProfile implements EnableHackListener {

    private final TreeMap<String, Boolean> hackList = new TreeMap<>();
    private EnabledHacksFile currentEnabledHacksFile;

    public ManagedHacks(String enabledHacksFilename) {
        this.profilesFolder = WurstClient.INSTANCE.getWurstFolder().resolve("enabled_hacks");
        System.out.println("ManagedHacks LOADING " + enabledHacksFilename);
        loadProfile(enabledHacksFilename);
        WurstClient.INSTANCE.getEventManager().add(EnableHackListener.class, this);
    }

    @Override
    public String getDisplayName() {
        return "EnabledHacks " + currentEnabledHacksFile.getBaseName();
    }

    public ArrayList<String> getHackList() {
        return new ArrayList<>(hackList.keySet());
    }

    public boolean isEnabled(int index){
        return hackList.get(getHackList().get(index));
    }

    public boolean isEnabled(String name){
        return hackList.get(name);
    }



    public void loadProfileHacks() {
        TreeMap<String, Boolean> currentSettings = currentEnabledHacksFile.loadHacks();
        WurstClient.INSTANCE.getHackRegistry().getAllHax().forEach(hack -> {
            if (!currentSettings.containsKey(hack.getName())){
                currentSettings.put(hack.getName(), Boolean.FALSE);
            }
        });
        if (WurstClient.isInGame){
            currentSettings.keySet().forEach(key -> {
                WurstClient.INSTANCE.getHackRegistry().getHackByName(key).setEnabled(currentSettings.get(key));
            });
        }
        this.hackList.putAll(currentSettings);
        updateProfileFile();
    }

    @Override
    public void loadProfile(String fileName) {
        if (currentEnabledHacksFile!=null){
            System.out.println("Existing profile detected, save and unload");
            unloadProfile();
        }
        currentEnabledHacksFile = new EnabledHacksFile(profilesFolder.resolve(fileName));
        System.out.println("loadProfile loadProfileHacks()");
        loadProfileHacks();
        currentEnabledHacksFile.save(this.createJson());
    }

    private void updateProfileFile(){
        System.out.println("Saveing hacks to file");
        currentEnabledHacksFile.save(this.createJson());
    }


    public void unloadProfile(){
        updateProfileFile();
        currentEnabledHacksFile = null;
        hackList.clear();
    }

    public void saveProfile(String fileName) throws IOException, JsonException {
        currentEnabledHacksFile = new EnabledHacksFile(profilesFolder.resolve(fileName));
        currentEnabledHacksFile.save(this.createJson());
    }

    @Override
    public JsonObject createJson() {
        JsonObject json = new JsonObject();
        WurstClient.INSTANCE.getHackRegistry().getAllHax().forEach(hack -> json.addProperty(hack.getName(), hack.isEnabled()));
        return json;
    }

    @Override
    public int size() {
        return this.hackList.size();
    }

    @Override
    public void setDefaults() {
        hackList.keySet().forEach(hack -> hackList.put(hack, Boolean.FALSE));
        updateProfileFile();
    }

    @Override
    public void onToggle(EnableHackEvent toggledHackEvent) {
        Hack toggledHack = toggledHackEvent.getToggledHack();
        hackList.put(toggledHack.getName(), toggledHack.isEnabled());
        updateProfileFile();
    }

    public void menuToggleEnabled(int selected) {
        String key = new ArrayList<>(hackList.keySet()).get(selected);
        Boolean state = hackList.get(key);
        hackList.put(key, !state);
        if (WurstClient.isInGame){
            WurstClient.INSTANCE.getHackRegistry().getHackByName(key).setEnabled(!state);
        }
        updateProfileFile();
    }
}
