package net.wurstclient.core;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

public class MatrixUtils {
    public static Matrix4f getPositionMatrix(MatrixStack matrixStack){
        return matrixStack.peek().getModel();
    }
}
