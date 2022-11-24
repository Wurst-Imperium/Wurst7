package net.wurstclient.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.wurstclient.WurstClient;

@Mixin(ClientWorld.class)
public class ClientWorldMixin{
	
	@Inject(method="getBlockParticle()Lnet/minecraft/block/Block;", at=@At("HEAD"), cancellable=true)
	private void getBlockParticle(CallbackInfoReturnable<Block> info)
	{
		if (WurstClient.INSTANCE.getHax().barrierEspHack.isEnabled()) {
			info.setReturnValue(Blocks.BARRIER);
		}
	}
}
