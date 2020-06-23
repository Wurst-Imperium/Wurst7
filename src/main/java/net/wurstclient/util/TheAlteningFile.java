package net.wurstclient.util;

import net.wurstclient.WurstClient;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

public class TheAlteningFile {
    private static final File theAlteningFile = new File(WurstClient.INSTANCE.getWurstFolder().toFile(), "thealtening.file");
    private static String apiKey = "";

    public static Optional<String> getAndUpdateApiKeyOrDefault(String key) {
        if (apiKey.isEmpty()) {
            if (!theAlteningFile.exists()) {
                if (key.isEmpty()) {
                    return Optional.empty();
                } else {
                    setApiKey(key);
                    return Optional.of(key);
                }
            } else {
                String fileApiKey = readTheAlteningFile();

                if (fileApiKey.isEmpty()) {
                    if (key.isEmpty()) {
                        return Optional.empty();
                    }
                    setApiKey(key);
                    return Optional.of(key);
                } else {
                    setApiKey(fileApiKey);
                    return Optional.of(apiKey);
                }
            }
        } else {
            if(!key.equals(apiKey) && !key.isEmpty()) {
                setApiKey(key);
            }
            return Optional.of(apiKey);
        }
    }

    public static void setApiKey(String apiKey) {
        TheAlteningFile.apiKey = apiKey;
        try {
            FileUtils.writeStringToFile(theAlteningFile, apiKey, Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(
                    "Couldn't save thealtening file.", e);
        }
    }

    private static String readTheAlteningFile() {
        if (!theAlteningFile.exists()) {
            try {
                if (!theAlteningFile.createNewFile()) {
                    throw new RuntimeException(
                            "Couldn't create thealtening file.");
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        "Couldn't create thealtening file.", e);
            }
            return "";
        }
        try {
            return FileUtils.readFileToString(theAlteningFile, Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }
}
