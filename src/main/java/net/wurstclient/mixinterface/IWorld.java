package net.wurstclient.mixinterface;

import java.util.List;
import net.minecraft.world.chunk.BlockEntityTickInvoker;

/**
 * IWorld
 *
 * @author Hexeption admin@hexeption.co.uk
 * @since 10/11/2020 - 04:38 am
 */
public interface IWorld {

    List<BlockEntityTickInvoker> getBlockEntities();
}
