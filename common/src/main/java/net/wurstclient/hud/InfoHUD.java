package net.wurstclient.hud;

import java.util.Map;
import java.util.Objects;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Position;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;

public class InfoHUD {
	private double speed;
	private Position prevPos;
	private long prevTime;
	private Map<String,Setting> settings;
	public InfoHUD() {
		settings = WurstClient.INSTANCE.getHackRegistry().hudHack.getSettings();
		prevTime = System.currentTimeMillis();
		speed = 0;
		prevPos = new Position(){
		
			@Override
			public double getZ() {
				return 0;
			}
		
			@Override
			public double getY() {
				return 0;
			}
		
			@Override
			public double getX() {
				return 0;
			}
		};
	}
	public void render(MatrixStack matrixStack){
		int screenHeight = WurstClient.MC.getWindow().getScaledHeight();
		int xPos = 2;
		int yOffset = 10;
		int yPos = screenHeight-yOffset;

		Position pos = WurstClient.MC.player.getPos();
		String dimension = String.valueOf(WurstClient.MC.world.getRegistryKey().getValue());
		if(((CheckboxSetting)settings.get("dimension coordinates")).isChecked()){
			int color;
			String dimension_pos;
			if(Objects.equals(dimension, "minecraft:the_nether")) {
				color = 0xffffffff;
				dimension_pos = "X: " + (int)Math.floor(pos.getX()*8) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()*8); 
			}
			else {
				color = 0xffff0000;
				dimension_pos =  "X: " + (int)Math.floor(pos.getX()/8) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()/8);
			}
			drawText(matrixStack, dimension_pos, xPos, yPos, color);
			yPos-=yOffset;
		}

		if(((CheckboxSetting)settings.get("coordinates")).isChecked()){
			String defpos = "X: " + (int)Math.floor(pos.getX()) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()); 
			int color;
			if(Objects.equals(dimension, "minecraft:the_nether")) {
				color = 0xffff0000;
			}
			else {
				color = 0xffffffff;
			}
			drawText(matrixStack, defpos, xPos, yPos, color);
			yPos -= yOffset;
		}
		//TODO: Calculate the speed more accurately
		if(((CheckboxSetting)settings.get("speed")).isChecked()){
			long currTime = System.currentTimeMillis();
			long deltaTime = currTime-prevTime;
			if(deltaTime > 500){
				double dist = Math.sqrt(Math.pow(Math.abs(pos.getX()-prevPos.getX()),2)+Math.pow(Math.abs(pos.getZ()-prevPos.getZ()),2));
				speed = (double)dist/((double)deltaTime/1000);
				prevPos = pos;
				prevTime = currTime;
			}
			drawText(matrixStack,Math.round(speed*10.0)/10.0 + " m/s", xPos, yPos, 0xffffffff);
			yPos-=yOffset;
		}
		if(((CheckboxSetting)settings.get("fps")).isChecked()) {
			drawText(matrixStack, WurstClient.MC.fpsDebugString.split(" fps")[0]+" FPS", xPos, yPos, 0xffffffff);
			yPos -= yOffset;
		}
	}

	public void drawText(MatrixStack matrixStack, String s, int x, int y, int color) {
		TextRenderer tr = WurstClient.MC.textRenderer;
		tr.draw(matrixStack, s, x, y, color);
	}
}