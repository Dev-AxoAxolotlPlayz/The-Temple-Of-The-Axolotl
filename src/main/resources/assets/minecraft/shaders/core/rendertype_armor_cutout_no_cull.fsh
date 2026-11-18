#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform float GameTime;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;

out vec4 fragColor;

// Only textures bearing the two invisible Axolite signature texels are animated.
float axoHash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453);
}
vec3 axoMaterial(vec3 base, vec2 uv, bool inner) {
    float t = GameTime * 1200.0;
    float phase = 6.2831853 * t / 8.0;
    float breath = 0.5 - 0.5*cos(phase);
    vec3 tint = mix(vec3(1.0,0.36,0.73),vec3(0.20,0.96,1.0),breath);
    vec2 p = uv * vec2(64.0,32.0);
    // Vanilla head UVs occupy the upper half of the left 32 texels.
    bool head = !inner && p.x < 32.0 && p.y < 16.0;
    bool sea = inner || (p.x < 16.0 && p.y >= 16.0);
    vec3 result = base * 0.64;
    if (sea) {
        float sway = sin(p.y*0.58 + phase)*0.75 + sin(p.y*0.23-phase*2.0)*0.35;
        float stem = 1.0-smoothstep(0.08,0.27,abs(fract((p.x+sway)/3.5)-0.5));
        float leaf = 0.5+0.5*sin(p.y*1.8+p.x*0.9+phase);
        float flow = sin(p.x*1.4+p.y*0.8-phase*2.0)+sin(p.x*0.6-p.y*1.5+phase);
        float caustic = pow(max(0.0,1.0-abs(flow)),5.0);
        result += tint*(stem*(0.22+0.24*leaf)+caustic*0.12);
    } else {
        vec2 cell = floor(p*0.8);
        vec2 d = fract(p*0.8)-0.5;
        float seed = axoHash(cell);
        float star = step(0.78,seed)*(1.0-smoothstep(0.04,0.22,length(d)))*(0.65+0.35*sin(phase*2.0+seed*31.0));
        float ribbon = sin(p.y*0.72 + sin(p.x*0.35+phase)*1.5 + phase);
        float aurora = pow(0.5+0.5*ribbon,6.0);
        result += tint*aurora*(head?0.13:0.40) + vec3(0.67,0.84,1.0)*star*0.7;
    }
    return clamp(result,0.0,1.0);
}

// AX-E03: six independent UV regions, assigned by the geometry bone.
vec3 axoTridentMaterial(vec3 base, vec2 uv) {
    vec2 pixel = uv * 256.0;
    int bone = int(floor(pixel.x / 128.0)) + 2 * int(floor(pixel.y / 64.0));
    vec2 p = mod(pixel, vec2(128.0,64.0));
    float phase = 6.2831853 * GameTime * 1200.0 / 8.0;
    float breath = 0.5 - 0.5*cos(phase);
    vec3 tint = mix(vec3(1.0,0.36,0.73),vec3(0.20,0.96,1.0),breath);
    vec2 cell = floor(p * 0.8);
    float seed = axoHash(cell);
    float star = step(0.78,seed)*(1.0-smoothstep(0.04,0.22,length(fract(p*0.8)-0.5)))
        *(0.65+0.35*sin(phase*2.0+seed*31.0));
    float ribbon = pow(0.5+0.5*sin(p.y*0.72+sin(p.x*0.35+phase)*1.5+phase),6.0);
    float flow = sin(p.x*1.4+p.y*0.8-phase*2.0)+sin(p.x*0.6-p.y*1.5+phase);
    float caustic = pow(max(0.0,1.0-abs(flow)),5.0);
    // The original atlas uses x=64..76 for cyan crystals.
    bool crystal = p.x >= 64.0 && p.x < 80.0;
    vec3 result = base * 0.92;
    if (crystal) {
        float luminance = max(base.r,max(base.g,base.b));
        result = mix(base, tint * max(0.50, luminance) * 1.45, 0.88);
        result += tint*(0.27*ribbon+0.20*caustic);
    } else if (bone == 0 || bone == 2) {
        result += tint*ribbon*0.32 + vec3(0.67,0.84,1.0)*star*0.65;
    } else {
        result += tint*(0.18*ribbon+0.08*caustic);
    }
    return clamp(result,0.0,1.0);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    ivec2 dims = textureSize(Sampler0, 0);
    if (all(equal(dims, ivec2(128,64)))) {
        ivec4 a = ivec4(round(texelFetch(Sampler0, ivec2(0,0),0)*255.0));
        ivec4 b = ivec4(round(texelFetch(Sampler0, ivec2(1,0),0)*255.0));
        if (all(equal(a,ivec4(17,93,211,0))) && b.r==231 && b.g==67 && b.a==0 && (b.b==1 || b.b==2)) {
            tex.rgb = axoMaterial(tex.rgb, texCoord0, b.b==2);
        }
    }
    if (all(equal(dims, ivec2(256,256)))) {
        ivec4 a = ivec4(round(texelFetch(Sampler0, ivec2(0,255),0)*255.0));
        ivec4 b = ivec4(round(texelFetch(Sampler0, ivec2(1,255),0)*255.0));
        if (all(equal(a,ivec4(17,93,211,0))) && all(equal(b,ivec4(231,67,3,0)))) {
            tex.rgb = axoTridentMaterial(tex.rgb, texCoord0);
        }
    }
    vec4 color = tex * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
