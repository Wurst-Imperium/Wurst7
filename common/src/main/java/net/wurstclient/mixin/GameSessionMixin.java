package net.wurstclient.mixin;


import net.minecraft.client.MinecraftClientGame;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.GameSessionEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClientGame.class)
public class GameSessionMixin {

    @Inject(at = {@At("TAIL")}, method = "onStartGameSession()V")
    public void onStartGameSession(CallbackInfo ci) {
        EventManager.fire(new GameSessionEventListener.GameSessionEvent(true));
    }

    @Inject(at = {@At("TAIL")}, method = "onLeaveGameSession()V")
    public void onLeaveGameSession(CallbackInfo ci) {
        EventManager.fire(new GameSessionEventListener.GameSessionEvent(false));
    }
}
