package net.wurstclient.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(LootPool.class)
public class LootPoolMixin {

    @Shadow
    @Final
    private Predicate<LootContext> predicate;

    @Shadow
    @Final
    private BiFunction<ItemStack, LootContext, ItemStack> javaFunctions;

    @Shadow
    @Final
    LootNumberProvider bonusRolls;

    @Shadow
    @Final
    LootNumberProvider rolls;

    @Shadow
    private void supplyOnce(Consumer<ItemStack> lootConsumer, LootContext context) {

    }

    @Overwrite
    public void addGeneratedLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
        System.out.println("MIXIN REACHED");
        if (context.getWorld() != null && !this.predicate.test(context)) {
            return;
        }
        Consumer<ItemStack> consumer = LootFunction.apply(this.javaFunctions, lootConsumer, context);
        int i = this.rolls.nextInt(context) + MathHelper.floor(this.bonusRolls.nextFloat(context) * context.getLuck());
        for (int j = 0; j < i; ++j) {
            this.supplyOnce(consumer, context);
        }
    }

}
