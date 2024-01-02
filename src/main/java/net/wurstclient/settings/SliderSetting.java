/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.text.DecimalFormat;
import java.util.LinkedHashSet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.minecraft.util.math.MathHelper;
import net.wurstclient.WurstClient;
import net.wurstclient.clickgui.Component;
import net.wurstclient.clickgui.components.SliderComponent;
import net.wurstclient.keybinds.PossibleKeybind;
import net.wurstclient.util.MathUtils;
import net.wurstclient.util.json.JsonUtils;

public class SliderSetting extends Setting implements SliderLock
{
	private double value;
	private final double defaultValue;
	private final double minimum;
	private final double maximum;
	private final double increment;
	private final ValueDisplay display;
	
	private SliderLock lock;
	private boolean disabled;
	private double usableMin;
	private double usableMax;
	
	public SliderSetting(String name, String description, double value,
		double minimum, double maximum, double increment, ValueDisplay display)
	{
		super(name, description);
		this.value = value;
		defaultValue = value;
		
		this.minimum = minimum;
		this.maximum = maximum;
		
		usableMin = minimum;
		usableMax = maximum;
		
		this.increment = increment;
		this.display = display;
	}
	
	public SliderSetting(String name, double value, double minimum,
		double maximum, double increment, ValueDisplay display)
	{
		this(name, "", value, minimum, maximum, increment, display);
	}
	
	@Override
	public final double getValue()
	{
		double value = isLocked() ? lock.getValue() : this.value;
		return MathUtils.clamp(value, usableMin, usableMax);
	}
	
	public final double getValueSq()
	{
		return MathHelper.square(getValue());
	}
	
	public final float getValueF()
	{
		return (float)getValue();
	}
	
	public final int getValueI()
	{
		return (int)getValue();
	}
	
	public final int getValueCeil()
	{
		return MathHelper.ceil(getValue());
	}
	
	/**
	 * Returns the actual value of a logarithmic slider,<br>
	 * calculated as <code>10 ^ {@link #getValueI()}</code>.
	 */
	public final int getValueLog()
	{
		return (int)Math.pow(10, getValueI());
	}
	
	public final double getDefaultValue()
	{
		return defaultValue;
	}
	
	public final String getValueString()
	{
		return display.getValueString(getValue());
	}
	
	public final void setValue(double value)
	{
		if(disabled || isLocked())
			return;
		
		setValueIgnoreLock(value);
	}
	
	private void setValueIgnoreLock(double value)
	{
		value = (int)Math.round(value / increment) * increment;
		value = MathUtils.clamp(value, usableMin, usableMax);
		
		this.value = value;
		update();
		
		WurstClient.INSTANCE.saveSettings();
	}
	
	public final void increaseValue()
	{
		setValue(getValue() + increment);
	}
	
	public final void decreaseValue()
	{
		setValue(getValue() - increment);
	}
	
	public final double getMinimum()
	{
		return minimum;
	}
	
	public final double getMaximum()
	{
		return maximum;
	}
	
	public final double getRange()
	{
		return maximum - minimum;
	}
	
	public final double getIncrement()
	{
		return increment;
	}
	
	public final double getPercentage()
	{
		return (getValue() - minimum) / getRange();
	}
	
	public float[] getKnobColor()
	{
		float f = (float)(2 * getPercentage());
		
		float red = MathUtils.clamp(f, 0, 1);
		float green = MathUtils.clamp(2 - f, 0, 1);
		float blue = 0;
		
		return new float[]{red, green, blue};
	}
	
	public final boolean isLocked()
	{
		return lock != null;
	}
	
	public final void lock(SliderLock lock)
	{
		this.lock = lock;
		update();
	}
	
	public final void unlock()
	{
		lock = null;
		update();
	}
	
	public final boolean isDisabled()
	{
		return disabled;
	}
	
	public final void setDisabled(boolean disabled)
	{
		this.disabled = disabled;
	}
	
	public final boolean isLimited()
	{
		return usableMax != maximum || usableMin != minimum;
	}
	
	public final double getUsableMin()
	{
		return usableMin;
	}
	
	public final void setUsableMin(double usableMin)
	{
		if(usableMin < minimum)
			throw new IllegalArgumentException("usableMin < minimum");
		
		this.usableMin = usableMin;
		update();
	}
	
	public final void resetUsableMin()
	{
		usableMin = minimum;
		update();
	}
	
	public final double getUsableMax()
	{
		return usableMax;
	}
	
	public final void setUsableMax(double usableMax)
	{
		if(usableMax > maximum)
			throw new IllegalArgumentException("usableMax > maximum");
		
		this.usableMax = usableMax;
		update();
	}
	
	public final void resetUsableMax()
	{
		usableMax = maximum;
		update();
	}
	
	@Override
	public final Component getComponent()
	{
		return new SliderComponent(this);
	}
	
	@Override
	public final void fromJson(JsonElement json)
	{
		if(!JsonUtils.isNumber(json))
			return;
		
		double value = json.getAsDouble();
		if(value > maximum || value < minimum)
			return;
		
		setValueIgnoreLock(value);
	}
	
	@Override
	public final JsonElement toJson()
	{
		return new JsonPrimitive(Math.round(value * 1e6) / 1e6);
	}
	
	@Override
	public JsonObject exportWikiData()
	{
		JsonObject json = new JsonObject();
		json.addProperty("name", getName());
		json.addProperty("descriptionKey", getDescriptionKey());
		json.addProperty("type", "Slider");
		
		json.addProperty("defaultValue", defaultValue);
		json.addProperty("minimum", minimum);
		json.addProperty("maximum", maximum);
		json.addProperty("increment", increment);
		
		return json;
	}
	
	@Override
	public final LinkedHashSet<PossibleKeybind> getPossibleKeybinds(
		String featureName)
	{
		String fullName = featureName + " " + getName();
		
		String command = ".setslider " + featureName.toLowerCase() + " ";
		command += getName().toLowerCase().replace(" ", "_") + " ";
		
		LinkedHashSet<PossibleKeybind> pkb = new LinkedHashSet<>();
		pkb.add(new PossibleKeybind(command + "more", "Increase " + fullName));
		pkb.add(new PossibleKeybind(command + "less", "Decrease " + fullName));
		
		return pkb;
	}
	
	public static interface ValueDisplay
	{
		public static final ValueDisplay INTEGER = v -> (int)v + "";
		
		public static final ValueDisplay DECIMAL =
			v -> Math.round(v * 1e6) / 1e6 + "";
		
		public static final ValueDisplay PERCENTAGE =
			v -> (int)(Math.round(v * 1e8) / 1e6) + "%";
		
		public static final ValueDisplay LOGARITHMIC = new ValueDisplay()
		{
			private static final DecimalFormat FORMAT =
				new DecimalFormat("#,###");
			
			@Override
			public String getValueString(double v)
			{
				return FORMAT.format(Math.pow(10, v));
			}
		};
		
		public static final ValueDisplay DEGREES = INTEGER.withSuffix("\u00b0");
		
		public static final ValueDisplay NONE = v -> "";
		
		public String getValueString(double value);
		
		public default ValueDisplay withLabel(double value, String label)
		{
			return v -> v == value ? label : getValueString(v);
		}
		
		public default ValueDisplay withPrefix(String prefix)
		{
			return v -> prefix + getValueString(v);
		}
		
		public default ValueDisplay withSuffix(String suffix)
		{
			return v -> getValueString(v) + suffix;
		}
	}
}
