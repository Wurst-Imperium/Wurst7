package net.wurstclient.hud;

import java.util.Map;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.util.math.Position;
import net.minecraft.world.dimension.DimensionType;
import net.wurstclient.WurstClient;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;

public class InfoHUD {
	private double speed;
	private Position prevPos;
	private long prevTime;
	private Map<String,Setting> settings;
	public InfoHUD() {
		settings = WurstClient.INSTANCE.getHax().hudHack.getSettings();
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
	public void renderer(float partialTicks) {
		int screenHeight = WurstClient.MC.getWindow().getScaledHeight();
		int xPos = 2;
		int yOffset = 10;
		int yPos = screenHeight-yOffset;

		Position pos = WurstClient.MC.player.getPos();
		
		if(((CheckboxSetting)settings.get("dimension coordinates")).isChecked()){
			int color;
			String dimension_pos;
			if(WurstClient.MC.player.dimension == DimensionType.THE_NETHER) {
				color = 0xffffffff;
				dimension_pos = "X: " + (int)Math.floor(pos.getX()*8) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()*8); 
			}
			else {
				color = 0xffff0000;
				dimension_pos = "X: " + (int)Math.floor(pos.getX()/8) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()/8); 
			}
			drawText(dimension_pos, xPos, yPos, color);
			yPos-=yOffset;
		}

		if(((CheckboxSetting)settings.get("coordinates")).isChecked()){
			String defpos = "X: " + (int)Math.floor(pos.getX()) + " Y: " + (int)Math.floor(pos.getY()) + " Z: " + (int)Math.floor(pos.getZ()); 
			int color;
			if(WurstClient.MC.player.dimension == DimensionType.THE_NETHER) {
				color = 0xffff0000;
			}
			else {
				color = 0xffffffff;
			}
			drawText(defpos, xPos, yPos, color);
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
			drawText(Math.round(speed*10.0)/10.0 + " m/s", xPos, yPos, 0xffffffff);
			yPos-=yOffset;
		}
		if(((CheckboxSetting)settings.get("fps")).isChecked()) {
			drawText(WurstClient.MC.fpsDebugString.split(" fps")[0]+" FPS", xPos, yPos, 0xffffffff);
			yPos -= yOffset;
		}
	}

	public void drawText(String s, int x, int y, int color) {
		TextRenderer tr = WurstClient.MC.textRenderer;

		tr.draw(s, x, y, color);
	}
}