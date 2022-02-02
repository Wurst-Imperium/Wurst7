package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.ShouldDrawSideListener;

@Pseudo
@Mixin(
	targets = "me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache",
	remap = false)
public class SodiumBlockOcclusionCacheMixin
{
	@Inject(method = "shouldDrawSide",
		at = @At("HEAD"),
		cancellable = true,
		remap = false)
	public void shouldDrawSide(BlockState state, BlockView view, BlockPos pos,
		Direction facing, CallbackInfoReturnable<Boolean> cir)
	{
		ShouldDrawSideListener.ShouldDrawSideEvent event =
			new ShouldDrawSideListener.ShouldDrawSideEvent(state);
		EventManager.fire(event);
		
		if(event.isRendered() != null)
			cir.setReturnValue(event.isRendered());
	}
}
