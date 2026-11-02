#version 150
#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform int FogShape;
uniform float GameTime;
uniform float TotaCameraY;
uniform float TotaSeaLevel;
uniform float TotaDepthRange;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 lightMapColor;
in float totaWater;
in vec2 totaWorldXZ;
in float totaWaveStrength;
in float totaWorldY;

out vec4 fragColor;

// PATCH D LOOK CONTROLS. All are visual-only; no extra textures or passes.
const float TOTA_WATER_OPACITY = 0.62;
const float TOTA_CYAN_GLOW = 1.35;
const float TOTA_ABYSS_DARKNESS = 0.78;
const float TOTA_TINT_DARKNESS = 0.62; // lower = darker, stronger contrast
const float TOTA_STAR_DENSITY = 0.23;   // fraction of procedural cells containing stars
const float TOTA_STAR_BRIGHTNESS = 0.90;
const float TOTA_TRAIL_BRIGHTNESS = 0.24;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float waveAt(vec2 xz, float t) {
    return 0.5 * sin(0.15*xz.x + 0.065*xz.y - 0.045*t)
         + 0.3 * sin(-0.095*xz.x + 0.12*xz.y - 0.032*t + 1.4)
         + 0.2 * sin(0.055*xz.x - 0.17*xz.y - 0.052*t + 2.1);
}

// World-anchored star cells, with stretched glints and antialiased centers.
// No texture sampling or additional render pass.
float starField(vec2 worldXZ, float t, out float paletteSeed) {
    vec2 uv = worldXZ * 2.2;
    vec2 cell = floor(uv);
    vec2 local = fract(uv) - 0.5;
    float seed = hash21(cell + 5.23);
    paletteSeed = seed;
    vec2 center = (vec2(hash21(cell + 13.7), hash21(cell + 41.9)) - 0.5) * 0.70;
    vec2 d = local - center;
    // Delicate stretched bioluminescent flecks, not large white dots.
    float r = length(d * vec2(0.75, 1.65));
    float aa = max(fwidth(r), 0.006);
    float core = 1.0 - smoothstep(0.018, 0.018 + aa * 1.7, r);
    float halo = 1.0 - smoothstep(0.035, 0.115 + aa, r);
    float starEnabled = step(1.0 - TOTA_STAR_DENSITY, seed);
    float shimmer = 0.80 + 0.20 * sin(t * 0.055 + seed * 23.0);
    return starEnabled * shimmer * (core + 0.22 * halo);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    vec4 color = tex * vertexColor * ColorModulator;
    if (color.a < 0.1) discard;

    if (totaWater > 0.5) {
        // The water marker is encoded in vertex alpha; do not use it for opacity.
        color.a = clamp(tex.a * ColorModulator.a * TOTA_WATER_OPACITY, 0.0, 0.90);
        // Abyss surface only: conceal the distant seabed from above while
        // retaining underwater translucency and the existing starry-water effects.
        // Sea level is 200 in the Abyss, 62 in the Overworld.
        bool abyssSurface = TotaSeaLevel > 100.0 &&
                            TotaCameraY >= TotaSeaLevel - 0.05 &&
                            totaWorldY >= TotaSeaLevel - 0.75;
        if (abyssSurface) {
            color.a = max(color.a, 0.975);
        }

        // Retain original biome tint while cooling/darkening the sea.
        // This is a dark multiplier, not a flat color replacement.
        // Scale depth across the 92-block Overworld and 200-block Abyss ranges.
        // Water faces below sea level and an underwater camera both contribute.
        float geometryDepth = max(0.0, TotaSeaLevel - totaWorldY);
        float cameraDepth = max(0.0, TotaSeaLevel - TotaCameraY);
        float normalizedDepth = clamp(max(geometryDepth, cameraDepth * 0.75)
                                      / max(1.0, TotaDepthRange), 0.0, 1.0);
        float depthCurve = smoothstep(0.0, 1.0, normalizedDepth);
        vec3 surfaceTint = vec3(0.63, 0.78, 0.93) * TOTA_TINT_DARKNESS;
        vec3 deepTint = vec3(0.16, 0.23, 0.46) * TOTA_ABYSS_DARKNESS;
        color.rgb *= mix(surfaceTint, deepTint, depthCurve);

        float t = GameTime * 24000.0;
        float wave = waveAt(totaWorldXZ, t);
        float crest = smoothstep(0.18, 0.68, wave);
        float seed;
        // Drift the stars gently with the same world-space wave function.
        // Project stars onto vertical curtains using the horizontal face axis
        // and world height; horizontal surfaces keep the original XZ pattern.
        // Derivatives select the dominant face orientation without changing geometry.
        float dx = length(vec2(dFdx(totaWorldXZ.x), dFdy(totaWorldXZ.x)));
        float dz = length(vec2(dFdx(totaWorldXZ.y), dFdy(totaWorldXZ.y)));
        float dy = length(vec2(dFdx(totaWorldY), dFdy(totaWorldY)));
        float curtain = step(max(dx, dz) * 0.55, dy);
        vec2 curtainCoords = vec2(dx >= dz ? totaWorldXZ.x : totaWorldXZ.y, totaWorldY);
        vec2 starCoords = mix(totaWorldXZ, curtainCoords, curtain)
                        + vec2(wave * 0.12, wave * -0.08);
        float stars = starField(starCoords, t, seed);

        // Narrow luminous ribbons follow the same crests as the vertex shader.
        // Their world-space modulation makes them ebb and flow with the waves.
        float ripple = sin(totaWorldXZ.x * 1.45 - totaWorldXZ.y * 0.76 + t * 0.018);
        float ribbon = pow(crest, 2.4) * (0.40 + 0.60 * smoothstep(-0.25, 0.9, ripple));
        float trail = ribbon * TOTA_TRAIL_BRIGHTNESS;

        // Visible in daylight; brighter after sunset. Never zero during day.
        float night = smoothstep(-0.25, 0.55, -cos(GameTime * 6.2831853));
        float dayNight = mix(0.65, 1.25, night);
        // Bright cyan centers and cool halos, with rare pale-white glints.
        vec3 starColor = mix(vec3(0.02, 0.93, 1.0), vec3(0.69, 1.0, 1.0), step(0.94, seed));
        // Fade the surface bioluminescence as the camera descends.
        float glowDepthFade = mix(1.0, 0.38, depthCurve);
        vec3 light = (starColor * stars * TOTA_STAR_BRIGHTNESS * TOTA_CYAN_GLOW
                    + vec3(0.01, 0.92, 1.0) * trail * TOTA_CYAN_GLOW) * glowDepthFade;

        // Apply ordinary water lighting first, then add subtle emissive light.
        // Keeping this inside the water branch avoids changing glass lighting.
        // Use a stable water-only illumination factor rather than the client lightmap.
        // The lightmap is brightened by Night Vision and pulses near expiry.
        // Terrain, entities, and non-water translucent blocks keep their lightmap.
        float stableWaterLight = mix(0.43, 0.86, 1.0 - night);
        color.rgb = clamp(color.rgb * stableWaterLight + light * dayNight, 0.0, 1.0);
    } else {
        color *= lightMapColor;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
