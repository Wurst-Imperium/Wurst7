/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;


import com.google.gson.JsonObject;
import net.minecraft.client.util.InputUtil;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

import javax.annotation.Nullable;

public abstract class ProfileFileReader {
    private final Path path;
    public boolean disableSaving = false;

    public ProfileFileReader(Path path) {
        if (!path.toString().toLowerCase().endsWith(".json")) {
            this.path = path.resolveSibling(path.getFileName() + ".json");
        } else {
            this.path = path;
        }
    }

    @Nullable
    public WsonObject load() {
        try {
            WsonObject obj = JsonUtils.parseFileToObject(path);
            return obj;

        } catch (NoSuchFileException e) {
            // The file doesn't exist yet. No problem, we'll create it.
            System.out.println("File " + path.getFileName() + " does not exist, creating empty file");
        } catch (IOException | JsonException e) {
            System.out.println("Couldn't load " + path.getFileName());
            e.printStackTrace();
            System.out.println("Purging file");
        }
        JsonObject ret = new JsonObject();
        save(ret);
        return new WsonObject(ret);
    }

    public void save(JsonObject object) {
        if (disableSaving) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            JsonUtils.toJson(object, path);

        } catch (IOException | JsonException e) {
            System.out.println("Couldn't save " + path.getFileName());
            e.printStackTrace();
        }
    }

    public String getBaseName(){
        return path.getFileName().toString().substring(0, path.getFileName().toString().toLowerCase(Locale.ROOT).indexOf(".json"));
    }
}
