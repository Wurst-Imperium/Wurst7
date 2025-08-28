package net.wurstclient.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.wurstclient.event.EventManager;
import net.wurstclient.events.GetPlayerDepthStriderListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/enchantment/EnchantmentHelper;getDepthStrider(Lnet/minecraft/entity/LivingEntity;)I"))
    private int getDepthStrider(LivingEntity entity){
        GetPlayerDepthStriderListener.GetPlayerDepthStriderEvent event = new GetPlayerDepthStriderListener.GetPlayerDepthStriderEvent(entity);
        EventManager.fire(event);
        return event.isCancelled()?3: EnchantmentHelper.getDepthStrider(entity);
    }
}
