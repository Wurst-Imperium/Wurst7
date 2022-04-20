package net.wurstclient.util.profiles;

import com.google.gson.JsonObject;
import net.wurstclient.util.json.JsonException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class ManagedProfile {

    public Path profilesFolder;

    public ArrayList<Path> listProfiles()
    {
        if(!Files.isDirectory(getProfilesFolder()))
            return new ArrayList<>();

        try(Stream<Path> files = Files.list(getProfilesFolder()))
        {
            return files.filter(Files::isRegularFile)
                    .collect(Collectors.toCollection(ArrayList::new));

        }catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public abstract String getDisplayName();

    public abstract void loadProfile(String fileName) throws IOException, JsonException;

    public abstract void saveProfile(String fileName) throws IOException, JsonException;

    public Path getProfilesFolder()
    {
        return profilesFolder;
    }

    public abstract JsonObject createJson();

    public abstract int size();

    public abstract void setDefaults();
}