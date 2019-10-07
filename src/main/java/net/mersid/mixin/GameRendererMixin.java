package net.mersid.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.mersid.events.CameraShakeEventListener;
import net.mersid.events.CameraShakeEventListener.CameraShakeEvent;
import net.minecraft.client.render.GameRenderer;
import net.wurstclient.WurstClient;

/**
 * Runs every tick to determine if the player is hurt. Canceling will prevent the camera shake, but does not tell anything about WHEN
 * the player gets hurt.
 * @author Mersid
 *
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	
	/**
	 * An event is fired here which, if canceled, will stop camera shake.
	 * @param partialTicks
	 * @param ci
	 */
	@Inject(at = @At("HEAD"),
			method = "bobViewWhenHurt",
			cancellable = true)
	private void onBobViewWhenHurt(float partialTicks, CallbackInfo ci)
	{
		CameraShakeEvent event = new CameraShakeEvent();
		WurstClient.INSTANCE.getEventManager().fire(event);
		
		if (event.isCancelled())
		{
			ci.cancel();
		}
	}
}
