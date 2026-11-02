#version 150
#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;
uniform float GameTime;
uniform vec2 TotaCameraXZ;
uniform float TotaCameraY;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 lightMapColor;
out float totaWater;
out vec2 totaWorldXZ;
out float totaWaveStrength;
out float totaWorldY;

float totaWave(vec2 xz, float t) {
    float a = sin(0.15 * xz.x + 0.065 * xz.y - 0.045 * t);
    float b = sin(-0.095 * xz.x + 0.12 * xz.y - 0.032 * t + 1.4);
    float c = sin(0.055 * xz.x - 0.17 * xz.y - 0.052 * t + 2.1);
    return 0.5 * a + 0.3 * b + 0.2 * c;
}

void main() {
    vec3 p = Position + ChunkOffset;
    float alphaByte = floor(Color.a * 255.0 + 0.5);
    totaWater = (alphaByte >= 230.0 && alphaByte <= 251.0) ? 1.0 : 0.0;
    totaWaveStrength = 0.0;
    totaWorldXZ = p.xz + TotaCameraXZ;
    totaWorldY = p.y + TotaCameraY;

    if (totaWater > 0.5 && alphaByte <= 250.0) {
        // 230..250 is the depth/amplitude marker written by Hotfix B.
        // Keep the established small geometry displacement while testing.
        float depthAmplitude = (alphaByte - 230.0) / 20.0;
        float amplitude = min(0.12, depthAmplitude * 0.12);
        float w = totaWave(totaWorldXZ, GameTime * 24000.0);
        totaWaveStrength = w;
        p.y += amplitude * w;
    }

    vec3 viewPosition = (ModelViewMat * vec4(p, 1.0)).xyz;
    gl_Position = ProjMat * vec4(viewPosition, 1.0);
    vertexDistance = length(viewPosition);
    vertexColor = Color;
    texCoord0 = UV0;
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
}
