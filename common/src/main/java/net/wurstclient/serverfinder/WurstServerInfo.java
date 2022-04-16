package net.wurstclient.serverfinder;

import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WurstServerInfo {
   public String name;
   public String address;
   public String playerCountLabel;
   public int playerCount;
   public int playercountMax;
   public String label;
   public long ping;
   public int protocolVersion = SharedConstants.getGameVersion().getProtocolVersion();
   public String version = null;
   public List<Text> playerListSummary = Collections.emptyList();
   private WurstServerInfo.ResourcePackState resourcePackState;
   @Nullable
   private String icon;
   private boolean local;

   public WurstServerInfo(String name, String address, boolean local) {
      this.resourcePackState = WurstServerInfo.ResourcePackState.PROMPT;
      this.name = name;
      this.address = address;
      this.local = local;
   }

   public NbtCompound serialize() {
      NbtCompound compoundTag = new NbtCompound();
      compoundTag.putString("name", this.name);
      compoundTag.putString("ip", this.address);
      if (this.icon != null) {
         compoundTag.putString("icon", this.icon);
      }

      if (this.resourcePackState == WurstServerInfo.ResourcePackState.ENABLED) {
         compoundTag.putBoolean("acceptTextures", true);
      } else if (this.resourcePackState == WurstServerInfo.ResourcePackState.DISABLED) {
         compoundTag.putBoolean("acceptTextures", false);
      }

      return compoundTag;
   }

   public WurstServerInfo.ResourcePackState getResourcePack() {
      return this.resourcePackState;
   }

   public void setResourcePackState(WurstServerInfo.ResourcePackState resourcePackState) {
      this.resourcePackState = resourcePackState;
   }

   public static WurstServerInfo deserialize(NbtCompound tag) {
      WurstServerInfo serverInfo = new WurstServerInfo(tag.getString("name"), tag.getString("ip"), false);
      if (tag.contains("icon", 8)) {
         serverInfo.setIcon(tag.getString("icon"));
      }

      if (tag.contains("acceptTextures", 1)) {
         if (tag.getBoolean("acceptTextures")) {
            serverInfo.setResourcePackState(WurstServerInfo.ResourcePackState.ENABLED);
         } else {
            serverInfo.setResourcePackState(WurstServerInfo.ResourcePackState.DISABLED);
         }
      } else {
         serverInfo.setResourcePackState(WurstServerInfo.ResourcePackState.PROMPT);
      }

      return serverInfo;
   }

   @Nullable
   public String getIcon() {
      return this.icon;
   }

   public void setIcon(@Nullable String string) {
      this.icon = string;
   }

   public boolean isLocal() {
      return this.local;
   }

   public void copyFrom(WurstServerInfo serverInfo) {
      this.address = serverInfo.address;
      this.name = serverInfo.name;
      this.setResourcePackState(serverInfo.getResourcePack());
      this.icon = serverInfo.icon;
      this.local = serverInfo.local;
   }

   @Environment(EnvType.CLIENT)
   public static enum ResourcePackState {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      private final Text name;

      private ResourcePackState(String name) {
         this.name = new TranslatableText("addServer.resourcePack." + name);
      }

      public Text getName() {
         return this.name;
      }
   }
}
