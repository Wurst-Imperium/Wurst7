package net.wurstclient.util;

import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicBoolean;

public class ModMenuUtils {


    public static boolean isModMenuPresent(){
        AtomicBoolean isModMenuPresent = new AtomicBoolean(FabricLoader.getInstance().isModLoaded("modmenu"));
        return isModMenuPresent.get();
    }
}
