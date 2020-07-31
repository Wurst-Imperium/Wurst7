package net.wurstclient.altmanager;

// Hi_ImKyle was here :)

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.wurstclient.util.SkinUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CompletableFuture;

public class PlayerSkinFetcher extends ResourceTexture {
    private static final Logger LOGGER = LogManager.getLogger();
    @Nullable
    private File cacheFile;
    private String url;
    private final boolean convertLegacy;
    @Nullable
    private final Runnable loadedCallback;
    @Nullable
    private CompletableFuture<?> loader;

    public static PlayerSkinFetcher Fetch(Identifier id, String username) throws IOException {
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        AbstractTexture abstractTexture = textureManager.getTexture(id);
        if (abstractTexture == null) {
            abstractTexture = new PlayerSkinFetcher((File)null, username, true, (Runnable)null);
            textureManager.registerTexture(id, (AbstractTexture)abstractTexture);
        }

        return (PlayerSkinFetcher)abstractTexture;
    }

    public PlayerSkinFetcher(@Nullable File cacheFile, String username, boolean convertLegacy, @Nullable Runnable callback) throws IOException {
        super(DefaultSkinHelper.getTexture(AbstractClientPlayerEntity.getOfflinePlayerUuid(username)));
        this.cacheFile = cacheFile;
        this.convertLegacy = convertLegacy;
        this.loadedCallback = callback;

        // Check if the url exists in the first place, should prevent the following issue:
        // https://github.com/Wurst-Imperium/Wurst7/pull/159#issuecomment-666946362
        URL tempUrl = SkinUtils.getSkinUrl(username);
        if(tempUrl == null){
            this.url = null;
            return;
        }
        this.url = tempUrl.toString();
    }

    private void onTextureLoaded(NativeImage image) {
        if (this.loadedCallback != null) {
            this.loadedCallback.run();
        }

        MinecraftClient.getInstance().execute(() -> {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> {
                    this.uploadTexture(image);
                });
            } else {
                this.uploadTexture(image);
            }

        });
    }

    private void uploadTexture(NativeImage image) {
        TextureUtil.allocate(this.getGlId(), image.getWidth(), image.getHeight());
        image.upload(0, 0, 0, true);
    }

    public void load(ResourceManager manager) throws IOException {

        // Causes extreme lag if the texture doesn't load as it continuously tries to load it.
//        MinecraftClient.getInstance().execute(() -> {
//            if (!this.loaded) {
//                try {
//                    super.load(manager);
//                } catch (IOException var3) {
//                    LOGGER.warn("Failed to load texture: {}", this.location, var3);
//                }
//
//                this.loaded = true;
//            }
//
//        });

        // Exit early if the skin url hasn't been found, either due to it being a Email or it just doesn't exist.
        // https://github.com/Wurst-Imperium/Wurst7/pull/159#issuecomment-666946362
        if(this.url == null) {
            return;
        }

        if (this.loader == null) {
            NativeImage nativeImage2;
            if (this.cacheFile != null && this.cacheFile.isFile()) {
                LOGGER.debug("Loading http texture from local cache ({})", this.cacheFile);
                FileInputStream fileInputStream = new FileInputStream(this.cacheFile);
                nativeImage2 = this.loadTexture(fileInputStream);
            } else {
                nativeImage2 = null;
            }

            if (nativeImage2 != null) {
                this.onTextureLoaded(nativeImage2);
            } else {
                this.loader = CompletableFuture.runAsync(() -> {
                    LOGGER.debug("Downloading http texture from {} to {}", this.url, this.cacheFile);

                    try {

                        Object inputStream2 = null;
                        URLConnection conn = new URL(this.url).openConnection();
                        try
                        {
                            inputStream2 = conn.getInputStream();
                            if (this.cacheFile != null) {
                                FileUtils.copyInputStreamToFile((InputStream) inputStream2, this.cacheFile);
                            }
                        }catch (IOException ioe){
                            LOGGER.warn("Failed to get input stream for http texture");
                        }

                        Object finalInputStream = inputStream2;
                        MinecraftClient.getInstance().execute(() -> {
                            NativeImage nativeImage = this.loadTexture((InputStream) finalInputStream);
                            if (nativeImage != null) {
                                this.onTextureLoaded(nativeImage);
                            }

                        });
                        return;
                    } catch (Exception var6) {
                        LOGGER.error("Couldn't download http texture", var6);
                        return;
                    }

                }, Util.getServerWorkerExecutor());
            }
        }
    }

    @Nullable
    private NativeImage loadTexture(InputStream stream) {
        NativeImage nativeImage = null;

        try {
            nativeImage = NativeImage.read(stream);
            if (this.convertLegacy) {
                nativeImage = remapTexture(nativeImage);
            }
        } catch (IOException var4) {
            LOGGER.warn("Error while loading the skin texture", var4);
        }

        return nativeImage;
    }

    private static NativeImage remapTexture(NativeImage image) {
        boolean bl = image.getHeight() == 32;
        if (bl) {
            NativeImage nativeImage = new NativeImage(64, 64, true);
            nativeImage.copyFrom(image);
            image.close();
            image = nativeImage;
            nativeImage.fillRect(0, 32, 64, 32, 0);
            nativeImage.copyRect(4, 16, 16, 32, 4, 4, true, false);
            nativeImage.copyRect(8, 16, 16, 32, 4, 4, true, false);
            nativeImage.copyRect(0, 20, 24, 32, 4, 12, true, false);
            nativeImage.copyRect(4, 20, 16, 32, 4, 12, true, false);
            nativeImage.copyRect(8, 20, 8, 32, 4, 12, true, false);
            nativeImage.copyRect(12, 20, 16, 32, 4, 12, true, false);
            nativeImage.copyRect(44, 16, -8, 32, 4, 4, true, false);
            nativeImage.copyRect(48, 16, -8, 32, 4, 4, true, false);
            nativeImage.copyRect(40, 20, 0, 32, 4, 12, true, false);
            nativeImage.copyRect(44, 20, -8, 32, 4, 12, true, false);
            nativeImage.copyRect(48, 20, -16, 32, 4, 12, true, false);
            nativeImage.copyRect(52, 20, -8, 32, 4, 12, true, false);
        }

        stripAlpha(image, 0, 0, 32, 16);
        if (bl) {
            stripColor(image, 32, 0, 64, 32);
        }

        stripAlpha(image, 0, 16, 64, 32);
        stripAlpha(image, 16, 48, 48, 64);
        return image;
    }

    private static void stripColor(NativeImage image, int x, int y, int width, int height) {
        int l;
        int m;
        for(l = x; l < width; ++l) {
            for(m = y; m < height; ++m) {
                int k = image.getPixelColor(l, m);
                if ((k >> 24 & 255) < 128) {
                    return;
                }
            }
        }

        for(l = x; l < width; ++l) {
            for(m = y; m < height; ++m) {
                image.setPixelColor(l, m, image.getPixelColor(l, m) & 16777215);
            }
        }

    }

    private static void stripAlpha(NativeImage image, int x, int y, int width, int height) {
        for(int i = x; i < width; ++i) {
            for(int j = y; j < height; ++j) {
                image.setPixelColor(i, j, image.getPixelColor(i, j) | -16777216);
            }
        }

    }
}

