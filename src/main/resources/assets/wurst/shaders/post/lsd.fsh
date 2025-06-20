#version 150

#moj_import <minecraft:globals.glsl>

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

layout(std140) uniform LSDConfig {
    vec2 Frequency;
    vec2 WobbleAmount;
};

out vec4 fragColor;

vec3 hue(float h)
{
    float r = abs(h * 6.0 - 3.0) - 1.0;
    float g = 2.0 - abs(h * 6.0 - 2.0);
    float b = 2.0 - abs(h * 6.0 - 4.0);
    return clamp(vec3(r,g,b), 0.0, 1.0);
}

vec3 HSVtoRGB(vec3 hsv) {
    return ((hue(hsv.x) - 1.0) * hsv.y + 1.0) * hsv.z;
}

vec3 RGBtoHSV(vec3 rgb) {
    vec3 hsv = vec3(0.0);
    hsv.z = max(rgb.r, max(rgb.g, rgb.b));
    float min = min(rgb.r, min(rgb.g, rgb.b));
    float c = hsv.z - min;

    if (c != 0.0)
    {
        hsv.y = c / hsv.z;
        vec3 delta = (hsv.z - rgb) / c;
        delta.rgb -= delta.brg;
        delta.rg += vec2(2.0, 4.0);
        if (rgb.r >= hsv.z) {
            hsv.x = delta.b;
        } else if (rgb.g >= hsv.z) {
            hsv.x = delta.r;
        } else {
            hsv.x = delta.g;
        }
        hsv.x = fract(hsv.x / 6.0);
    }
    return hsv;
}

void main() {
    float adjustedTime = GameTime * 1200.0;
    float xOffset = sin(texCoord.y * Frequency.x + adjustedTime * 3.1415926535 * 2.0) * WobbleAmount.x;
    float yOffset = cos(texCoord.x * Frequency.y + adjustedTime * 3.1415926535 * 2.0) * WobbleAmount.y;
    vec2 offset = vec2(xOffset, yOffset);
    vec4 rgb = texture(DiffuseSampler, texCoord + offset);
    vec3 hsv = RGBtoHSV(rgb.rgb);
    hsv.x = fract(hsv.x + adjustedTime);
    fragColor = vec4(HSVtoRGB(hsv), 1.0);
}
